import java.io.*
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.locks.Condition
import kotlin.jvm.internal.FunctionReference
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf

val regex = "^(-?[1-9][0-9]*(([.][0-9]+)?([eE][+-]?[0-9]+)?)?)|^(-?0[.][0-9]+([eE][+-]?[0-9]+)?)|^(0)".toRegex()

//TODO - Need to handle how to access lists in Scheme

val standard_env = mutableMapOf<String, Any>()

fun main(args: Array<String>) {
    initEnvironment()
    val scanner:Scanner = Scanner(System.`in`)
    while(true){
        var code = scanner.nextLine()
        do {
            val result = evalExpression(code)
            println(result?.first)
            code = if (result != null) result.second else ""

        } while (code.length > 0)
    }
//    var text = File("./input.txt").readText()
//    do {
//        val result = evalExpression(text)
//        println(result?.first)
//        text = if (result != null) result.second else ""
//
//    } while (text.length > 0)
}

//TODO - The signature of these functions should be numList:List<>, operator:String -> Any ----- So that when calling them it would be easy
//TODO - So change these functions evalArithmetic and evalTest to those
fun initEnvironment() {
    standard_env.put("pi", 22.0 / 7.0)
    standard_env.put("+", ::evalArithmetic)
    standard_env.put("-", ::evalArithmetic)
    standard_env.put("*", ::evalArithmetic)
    standard_env.put("/", ::evalArithmetic)
    standard_env.put(">", ::evalTest)
    standard_env.put("<", ::evalTest)
    standard_env.put(">=", ::evalTest)
    standard_env.put("<=", ::evalTest)
    standard_env.put("=", ::evalTest)
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
        return evalIfExpression(current.substring(2))
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
        else{
            remString = ")"
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
    val ret = callTheProcess(proc,string,expParts[0])
    if(ret!=null) return ret
    if(proc!=null){
        var key = ""
        while(proc!=null&&proc !is KFunction<*>){
            key = proc as String
            proc = standard_env.get(proc)
        }
        return callTheProcess(proc,string,expParts[0])
    }
    return null
}

fun callTheProcess(proc:Any?, arg: String, operator: String): Pair<Any, String>?{
    if (proc != null && proc is KFunction<*>) {
        try{
            val pair = proc.call(arg, operator[0]) as Pair<Any, String>?
            if(pair!=null){
                return pair
            }
        }catch (e: IllegalArgumentException){

        }

        return proc.call(arg,operator) as Pair<Any, String>
    }
    return null
}

//TODO - Need to calculate - when it is used as an unary operator
//TODO - Need to change the following method..so that it will only do arithmetic and not parsing
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
            if(value is String && value==varName){
                return Pair(Unit, varValue.second.trimStart().substring(1))
            }
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

fun evalTest(string:String, condition: String) : Pair<Any,String>?{
    var strings = string.split(" ", limit = 2)
    if(strings.size>0){
        val first = evalExpression(strings[1].trimStart())
        val second = evalExpression(first!!.second.trimStart())

        if(first.first is Double && second!!.first is Double && second.second.trimStart().startsWith(")")){
            val testResult = when(condition){
                "=" -> first.first as Double == second.first as Double
                ">" -> first.first as Double > second.first as Double
                "<" -> (first.first as Double) < second.first as Double
                "<=" -> first.first as Double <= second.first as Double
                ">=" -> first.first as Double >= second.first as Double
                else -> false
            }
            return Pair(testResult,second.second.trimStart().substring(1))
        }
    }
    return null
}

//TODO - I shouldn't evaluate both the expressions of the If condition
fun evalIfExpression(string: String): Pair<Any, String>? {
    val test = evalExpression(string.trimStart())
    if(test!=null){
        val conseq = evalExpression(test!!.second.trimStart())
        val alt = evalExpression(conseq!!.second.trimStart())

        if((test.first is Double) || (test.first as Boolean) == true){
            val conseq = evalExpression(test!!.second.trimStart())
            return Pair(conseq!!.first,alt!!.second.trimStart().substring(1))
        }
        return Pair(alt!!.first,alt!!.second.trimStart().substring(1))
    }
    return null
}

fun identifyAndReturnExpression(string: String): String {

    return ""
}