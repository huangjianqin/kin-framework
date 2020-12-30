package org.kin.framework.ideascript

import com.thoughtworks.qdox.JavaProjectBuilder

import java.util.stream.Collectors

/**
 * builder方法生成脚本
 *
 * @author huangjianqin
 * @date 2020/12/30
 */
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
if(typeParameters != null && typeParameters.size() > 0) {
    //带泛型参数的类名
    className = className + "<" + sj.toString() + ">"
}

// key -> field name, value -> field type
def fieldName2Types = clazz.getFields()
        .stream()
        .filter({ r -> !r.isStatic() && !r.isFinal() })
        .map({ r -> new AbstractMap.SimpleEntry(r.getName(), r.getType().getGenericValue()) })
        .collect(Collectors.toList())
//生成字段赋值方法
def sb = new StringBuilder()
for (field in fieldName2Types) {
    sb.append(String.format("public %s %s(%s %s){\r\n", className, field.getKey(), field.getValue(), field.getKey()))
            .append(String.format("        this.%s = %s;\r\n", field.getKey(), field.getKey()))
            .append("        return this;\r\n")
            .append("}\r\n")
}

return sb.toString()
