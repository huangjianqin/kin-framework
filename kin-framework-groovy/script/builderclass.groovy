package org.kin.framework.ideascript

import com.thoughtworks.qdox.JavaProjectBuilder

import java.util.stream.Collectors

/**
 * builder内部类生成脚本
 *
 * @author huangjianqin
 * @date 2020/12/30
 */
//首字母小写转换
 def firstLowerCase(String s) {
     char[] chars = s.toCharArray();
     if (chars[0] >= 'A' && chars[0] <= 'Z') {
         chars[0] = (char) (chars[0] + 32);
     }
     return new String(chars);
 }

def builder = new JavaProjectBuilder()
builder.setEncoding("UTF-8")
//当前idea正在编辑的文件
def file = new File(_editor.getVirtualFile().getPath())
builder.addSource(file)
def clazz = builder.getClasses().iterator().next()
//idea正在编辑的.java文件对应的类名
def className = clazz.getSimpleName()

//泛型参数
StringJoiner sj = new StringJoiner(",")
def typeParameters = clazz.getTypeParameters()
for(typeParameter in typeParameters) {
    sj.add(typeParameter.getName())
}
//钻石符号描述
def typeParameterStr = "";
if(typeParameters != null && typeParameters.size() > 0) {
    typeParameterStr = "<" + sj.toString() + ">"
    //带泛型参数的类名
    className = className + typeParameterStr
}

// key -> field name, value -> field type
def fieldName2Types = clazz.getFields()
        .stream()
        .filter({ r -> !r.isStatic() && !r.isFinal() })
        .map({ r -> new AbstractMap.SimpleEntry(r.getName(), r.getType().getGenericValue()) })
        .collect(Collectors.toList())

def firstLowerCaseClassName = firstLowerCase(clazz.getSimpleName())
def sb = new StringBuilder()
//builder static method
sb.append("public static Builder builder(){ return new Builder();}\n")
sb.append("\n")
//注释
sb.append("/** builder **/\n")
//类声明
sb.append("public static class Builder" + typeParameterStr + " {\r\n")
//字段
sb.append(String.format("        private %s %s = new %s();\r\n\r\n", className, firstLowerCaseClassName, className))
//生成字段赋值方法
for (field in fieldName2Types) {
    sb.append(String.format("        public %s %s(%s %s){\r\n", "Builder" + typeParameterStr, field.getKey(), field.getValue(), field.getKey()))
            .append(String.format("                %s.%s = %s;\r\n", firstLowerCaseClassName, field.getKey(), field.getKey()))
            .append("                return this;\r\n")
            .append("        }\r\n")
}
//生成build方法
sb.append(String.format("        public %s build(){\r\n", className))
        .append(String.format("                return %s;\r\n", firstLowerCaseClassName))
        .append("        }\r\n")

sb.append("    }")

return sb.toString()
