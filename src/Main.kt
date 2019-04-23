import java.io.*
import kotlin.jvm.internal.FunctionReference
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf

val regex = "^(-?[1-9][0-9]*(([.][0-9]+)?([eE][+-]?[0-9]+)?)?)|^(-?0[.][0-9]+([eE][+-]?[0-9]+)?)|^(0)".toRegex()

val standard_env = mutableMapOf<String, Any>()

fun main(args: Array<String>) {
    initEnvironment()

    var text = File("./input.txt").readText()
    do {
        val result = evalExpression(text)
        println(result?.first)
        text = if (result != null) result.second else ""

    } while (text.length > 0)
}

fun initEnvironment() {
    standard_env.put("pi", 22.0 / 7.0)
    standard_env.put("+", ::evalArithmetic)
    standard_env.put("-", ::evalArithmetic)
    standard_env.put("*", ::evalArithmetic)
    standard_env.put("/", ::evalArithmetic)
}

fun evalExpression(string: String): Pair<Any, String>? {

    var pair: Pair<Any, String>? = evalNumber(string)
    if (pair != null) return pair

    pair = evalSymbol(string)
    if (pair != null) return pair

    var current = string.trimStart()
    if (current[0] != '(') return null

    current = current.substring(1).trimStart()
    if (current.startsWith("define")) {
        return evalDefineExpression(current.substring(6))
    }

    if (current.startsWith("if")) {
        return evalIfExpression(string)
    }

    return evalProc(current)

}

fun evalNumber(string: String): Pair<Double, String>? {
    val num = regex.find(string)?.value
    if (num != null) {
        if (string.length > num.length) {
            if (string[num.length] == ' ' || string[num.length] == ')')
                return Pair(num.toDouble(), string.subSequence(num.length, string.length).toString())
            return null
        }
        return Pair(num.toDouble(), string.subSequence(num.length, string.length).toString())
    }
    return null
}

//The trim() function here is called because when reading a file a null character will be attached at the end of the string
//TODO - Check for the validity of the symbol naming convention
fun evalSymbol(string: String): Pair<Any, String>? {
    var symbol = string.trimStart().split(" ", "\n", limit = 2)
    var value: Any?
    var remString = ""
    lateinit var key: String
    if (symbol.size > 1)
        remString = symbol[1]
    if (symbol[0].endsWith(")") && symbol[0].length > 1) {
        key = symbol[0].trim().substring(0, symbol[0].length - 1)
        value = standard_env.get(key)
        if (symbol.size > 1) {
            remString = ") " + symbol[1]
        }
    } else {
        key = symbol[0].trim()
        value = standard_env.get(key)
    }
    if (value != null) {
        return when (value) {
            is Double -> Pair(value, remString)
            is KFunction<*> -> Pair("<Function $key>", remString)
            is String -> Pair(value, remString)
            else -> null
        }
    }
    return null
}

fun evalProc(string: String): Pair<Any, String>? {
    val expParts = string.trimStart().split(" ", limit = 2)
    var proc = standard_env.get(expParts[0])
    if (proc != null && proc is KFunction<*>) {
        return proc.call(string, expParts[0][0]) as Pair<Any, String>?
    }
    if(proc!=null){
        var key = ""
        while(proc!=null&&proc !is KFunction<*>){
            key = proc as String
            proc = standard_env.get(proc)
        }
        if(proc!=null){
            return (proc as KFunction<*>).call(string,key[0]) as Pair<Any, String>?
        }
    }
    return null
}

fun evalArithmetic(string: String, operator: Char): Pair<Any, String>? {
    var strings = string.split(" ", limit = 2)
    if (strings.size > 1) {
        var current = strings[1]

        val exprPair = evalExpression(current)
        if (exprPair == null || exprPair.first !is Double) return null
        var expValue = exprPair.first as Double
        current = exprPair.second.trimStart()

        while (!current.startsWith(')')) {
            val operand = evalExpression(current)
            if (operand == null || operand.first !is Double) return null
            try {
                when (operator) {
                    '+' -> expValue += operand.first as Double
                    '-' -> expValue -= operand.first as Double
                    '*' -> expValue *= operand.first as Double
                    '/' -> expValue /= operand.first as Double
                }
            } catch (e: NumberFormatException) {
                println(e.stackTrace)
                return null
            }
            current = operand.second.trimStart()
        }
        return Pair(expValue, current.substring(1))
    }
    return null
}

//TODO - Check for the validity of the symbol naming convention
fun evalDefineExpression(string: String): Pair<Unit, String>? {
    val expParts = string.trimStart().split(" ", limit = 2)
    val varName = expParts[0]
    val varValue = evalExpression(expParts[1])
    if (varValue != null && varValue.second.trimStart().startsWith(")")) {
        var value: Any?
        value = varValue.first
        if(value!=null && value !is Double){
            value = expParts[1].split(" ","\n",")", limit = 2)[0]
        }
        if (value != null) {
            if (standard_env.get(varName) != null) {
                standard_env.replace(varName, value)
                return Pair(Unit, varValue.second.trimStart().substring(1))
            }
            standard_env.put(varName, value)
            return Pair(Unit, varValue.second.trimStart().substring(1))
        }
    }
    return null
}

fun evalIfExpression(string: String): Pair<Any, String>? {


    return null
}