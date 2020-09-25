package org.kin.framework.ideascript

import com.thoughtworks.qdox.JavaProjectBuilder

import java.util.stream.Collectors

/**
 * 工厂方法生成脚本
 *
 * @author huangjianqin* @date 2020/9/25
 */
def builder = new JavaProjectBuilder()
builder.setEncoding("UTF-8")
def file = new File(_editor.getVirtualFile().getPath())
def className = file.getName().split("\\.")[0]
builder.addSource(file)
def clazz = builder.getClasses().iterator().next()

def fieldTypeNames = clazz.getFields()
        .stream()
        .filter({ r -> !r.isStatic() })
        .map({ r -> new AbstractMap.SimpleEntry(r.getName(), r.getType().getGenericValue()) })
        .collect(Collectors.toList())
def argsStr = fieldTypeNames.stream().map({ r -> r.getValue() + " " + r.getKey() }).collect(Collectors.joining(", "))

def sb = new StringBuilder()
sb.append(String.format('public static %s of(%s){ \r\n', className, argsStr))
sb.append(String.format('        %s inst = new %s(); \r\n', className, className))
for (field in fieldTypeNames) {
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