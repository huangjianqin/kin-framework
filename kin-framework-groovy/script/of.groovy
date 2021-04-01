package org.kin.framework.ideascript

import com.thoughtworks.qdox.JavaProjectBuilder

import java.util.stream.Collectors

/**
 * 工厂方法生成脚本
 *
 * @author huangjianqin
 * @date 2020/9/25
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
        .filter({ r -> !r.isStatic() })
        .map({ r -> new AbstractMap.SimpleEntry(r.getName(), r.getType().getGenericValue()) })
        .collect(Collectors.toList())
def argsStr = fieldName2Types.stream().map({ r -> r.getValue() + " " + r.getKey() }).collect(Collectors.joining(", "))

def sb = new StringBuilder()
//方法头
sb.append(String.format('public static%s %s of(%s){ \r\n', " " + typeParameterStr, className, argsStr))
//实例化
sb.append(String.format('        %s inst = new %s(); \r\n', className, className))
//生成字段赋值代码块
for (field in fieldName2Types) {
    sb.append("        inst.")
            .append(field.getKey())
            .append(" = ")
            .append(field.getKey())
            .append(";")
            .append('\r\n')
}
sb.append('        return inst; \r\n')
sb.append("    }")
return sb.toString()