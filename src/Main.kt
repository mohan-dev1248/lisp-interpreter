import java.util.*
import kotlin.reflect.KFunction

val regex = "^(-?[1-9][0-9]*(([.][0-9]+)?([eE][+-]?[0-9]+)?)?)|^(-?0[.][0-9]+([eE][+-]?[0-9]+)?)|^(0)".toRegex()

//TODO - Need to handle how to access lists in Scheme

val global_env: Environment = Environment()

//Since kotlin 1.3 parameters args:Array<String> is not necessary
fun main() {
    initGlobalEnvironment()
    val scanner = Scanner(System.`in`)
    while (true) {
        var code = scanner.nextLine()
        do {
            val result = evalExpression(string = code)
            if (result != null && result.first is Pair<*, *>) {
                println((result.first as Pair<Procedure, String>).first)
            } else {
                println(result?.first)
            }
            code = result?.second ?: ""

        } while (code.isNotEmpty()) // code.isNotEmpty() is similar to code.length>0
    }
//    var text = File("./input.txt").readText()
//    do {
//        val result = evalExpression(text)
//        println(result?.first)
//        text = if (result != null) result.second else ""
//
//    } while (text.length > 0)
}

//TODO - Need to add other global environment variables
fun initGlobalEnvironment() {
    val standardEnv = mutableMapOf<String, Any>()
    standardEnv["pi"] = 22.0 / 7.0
    standardEnv["+"] = ::evalArithmetic
    standardEnv["-"] = ::evalArithmetic
    standardEnv["*"] = ::evalArithmetic
    standardEnv["/"] = ::evalArithmetic
    standardEnv[">"] = ::evalTest
    standardEnv["<"] = ::evalTest
    standardEnv[">="] = ::evalTest
    standardEnv["<="] = ::evalTest
    standardEnv["="] = ::evalTest
    global_env.init(standardEnv)
}

fun evalExpression(env: Environment = global_env, string: String): Pair<Any, String>? {

    if (string.isEmpty()) return null
    var pair: Pair<Any, String>? = evalNumber(env, string)
    if (pair != null) return pair

    pair = evalSymbol(env, string)
    if (pair != null) return pair

    var current = string.trimStart()
    if (current[0] != '(') return null

    current = current.substring(1).trimStart()
    if (current.startsWith("define"))
        return evalDefineExpression(env, current.substring(6))
    if (current.startsWith("if"))
        return evalIfExpression(env, current.substring(2))
    if (current.startsWith("lambda"))
        return evalLambdaExpression(env, current.substring(6))
    if (current.startsWith("quote"))
        return evalQuoteExpression(current.substring(5))
    if (current.startsWith("begin"))
        return evalBeginExpression(env, current.substring(5))
    return evalProc(env, current)

}

fun evalNumber(env: Environment, string: String): Pair<Double, String>? {
    val num = regex.find(string)?.value
    if (num == null) return null
    if (string.length > num.length) {
        if (string[num.length] == ' ' || string[num.length] == ')')
            return Pair(num.toDouble(), string.subSequence(num.length, string.length).toString())
        return null
    }
    return Pair(num.toDouble(), string.subSequence(num.length, string.length).toString())
}

//The trim() function here is called because when reading a file a null character will be attached at the end of the string
fun evalSymbol(env: Environment, string: String): Pair<Any, String>? {
    val symbol = string.trimStart().split(" ", "\n", limit = 2)
    var value: Any?
    var remString = ""
    lateinit var key: String
    if (symbol.size > 1)
        remString = symbol[1]
    if (symbol[0].contains(")") && symbol[0].length > 1 && !symbol[0].contains("(")) {

        key = symbol[0].trim().substring(0, symbol[0].indexOf(")"))
        value = env.find(key)
        remString = if (symbol.size > 1) {
            symbol[0].substring(symbol[0].indexOf(")")) + symbol[1]
        } else {
            symbol[0].substring(symbol[0].indexOf(")"))
        }
    } else if (symbol[0].contains("(") && !symbol[0].startsWith("(")) {

        key = symbol[0].substring(0, symbol[0].indexOf("("))
        val argumentExpression = identifyAndReturnExpression(string.substring(symbol[0].indexOf("(")))
        remString = argumentExpression.second

        value = Pair(env.find(key), argumentExpression.first)
    } else {
        key = symbol[0].trim()
        value = env.find(key)
    }
    if (value == null) return null
    return when (value) {
        is Double -> Pair(value, remString)
        is KFunction<*> -> Pair("<Function $key>", remString)
        is String -> Pair(value, remString)
        is Pair<*, *> -> Pair(value, remString)
        is Procedure -> {
            if (symbol.size > 1) {
                val argumentExpression = identifyAndReturnExpression(symbol[1])
                if (argumentExpression.first.startsWith("(")) {
                    value = Pair(value, argumentExpression.first)
                    remString = argumentExpression.second
                    Pair(value, remString)
                } else
                    null
            } else null
        }
        else -> null
    }
}

fun evalProc(env: Environment, string: String): Pair<Any, String>? {
    val proced = evalSymbol(env, string)
    if (proced != null && proced.first is Pair<*, *>) {
        val procedure = (proced.first as Pair<*, *>).first as Procedure
        val argumentExpression = (proced.first as Pair<*, *>).second as String

        if (argumentExpression.isEmpty()) return null

        val singleExpr = evalExpression(env, argumentExpression)
        if (singleExpr != null && singleExpr.first is Double) {
            val procEvaluated = procedure.call(singleExpr.first as Double, outerEnv = env)
            if (procEvaluated != null && proced.second.trimStart().startsWith(")"))
                return Pair(procEvaluated.first, proced.second.trimStart().substring(1))
            return null
        }

        val argumentList = mutableListOf<Double>()
        val arguments = argumentExpression.substring(1)

        var argument = identifyAndReturnExpression(arguments)
        val value = evalExpression(env, argument.first) ?: return null
        argumentList.add(value.first as Double)

        while ((argument.second) != ")") {
            argument = identifyAndReturnExpression(argument.second)
            val value = evalExpression(env, argument.first)
            argumentList.add(value!!.first as Double)
        }

        val procEvaluated = procedure.call(*(argumentList.toList().toDoubleArray()), outerEnv = env)
        return if (procEvaluated != null && proced.second.trimStart().startsWith(")"))
            Pair(procEvaluated.first, proced.second.trimStart().substring(1))
        else null
    }
    val expParts = string.trimStart().split(" ", limit = 2)
    var proc = env.find(expParts[0])
    if (proc != null) {
        if (proc is KFunction<*>)
            return proc.call(env, string, expParts[0]) as Pair<Any, String>?
        var key = ""
        while (proc != null && proc !is KFunction<*>) {
            key = proc as String
            proc = env.find(proc)
        }
        if (proc != null && proc is KFunction<*>)
            return proc.call(env, string, key) as Pair<Any, String>?
    }
    return null
}

//TODO - Need to calculate - when it is used as an unary operator
//TODO - Need to change the following method..so that it will only do arithmetic and not parsing
fun evalArithmetic(env: Environment, string: String, operator: String): Pair<Any, String>? {
    val strings = string.split(" ", limit = 2)
    if (strings.size > 1) {
        var current = strings[1]

        val exprPair = evalExpression(env, current)
        if (exprPair == null || exprPair.first !is Double) return null
        var expValue = exprPair.first as Double
        current = exprPair.second.trimStart()

        while (!current.startsWith(')')) {
            val operand = evalExpression(env, current)
            if (operand == null || operand.first !is Double) return null
            try {
                when (operator) {
                    "+" -> expValue += operand.first as Double
                    "-" -> expValue -= operand.first as Double
                    "*" -> expValue *= operand.first as Double
                    "/" -> expValue /= operand.first as Double
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
fun evalDefineExpression(env: Environment, string: String): Pair<Unit, String>? {
    val expParts = string.trimStart().split(" ", limit = 2)
    val varName = expParts[0]
    if (!validSymbolName(varName)) return null
    val varValue = evalExpression(env, expParts[1])
    if (varValue != null && varValue.second.trimStart().startsWith(")")) {
        var value = varValue.first
        if (value is String) {
            if (value == varName)
                return Pair(Unit, varValue.second.trimStart().substring(1))
            value = expParts[1].split(" ", "\n", ")", limit = 2)[0]
        }
        //The below if condition replaces the value of a key - but if !set is implemented this should go there
        if (env.find(varName) != null) {
            env.replace(varName, value)
            return Pair(Unit, varValue.second.trimStart().substring(1))
        }
        env.add(varName, value)
        return Pair(Unit, varValue.second.trimStart().substring(1))
    }
    return null
}

fun validSymbolName(sname: String): Boolean {
    val regex = "^[a-zA-Z_][a-zA-Z0-9_]+".toRegex()
    return regex.matches(sname)
}

fun evalTest(env: Environment, string: String, condition: String): Pair<Any, String>? {
    val strings = string.split(" ", limit = 2)
    if (strings.isNotEmpty()) {
        val first = evalExpression(env, strings[1].trimStart())
        val second = evalExpression(env, first!!.second.trimStart())

        if (first.first is Double && second!!.first is Double && second.second.trimStart().startsWith(")")) {
            val testResult = when (condition) {
                "=" -> first.first as Double == second.first as Double
                ">" -> first.first as Double > second.first as Double
                "<" -> (first.first as Double) < second.first as Double
                "<=" -> first.first as Double <= second.first as Double
                ">=" -> first.first as Double >= second.first as Double
                else -> false
            }
            return Pair(testResult, second.second.trimStart().substring(1))
        }
    }
    return null
}

fun evalIfExpression(env: Environment, string: String): Pair<Any, String>? {
    val test = evalExpression(env, string.trimStart())
    if (test != null) {
        val conseq = identifyAndReturnExpression(test.second)
        val alt = identifyAndReturnExpression(conseq.second)

        if (test.first is Double || test.first as Boolean) {
            val conseqVal = evalExpression(env, conseq.first)
            return if (conseqVal != null) Pair(conseqVal.first, alt.second.trimStart().substring(1)) else null
        }

        if (!(test.first as Boolean)) {
            val altValue = evalExpression(env, alt.first)
            return if (altValue != null) Pair(altValue.first, alt.second.trimStart().substring(1)) else null
        }
    }
    return null
}

//First one of the pair is the expression identified and the second one is the remaining String
//If found an invalid expression this function returns Pair("","")
fun identifyAndReturnExpression(inp: String): Pair<String, String> {
    val string = inp.trim()
    var expression = string
    if (string.startsWith("(")) {
        val listOfBraces = mutableListOf('(')
        expression = "("
        var parsedIndex = 1
        while (listOfBraces.size != 0 && parsedIndex < string.length) {
            if (string[parsedIndex] == '(' && string[parsedIndex - 1] == '(')
                return Pair("", "")
            if (string[parsedIndex] == '(')
                listOfBraces.add('(')
            if (string[parsedIndex] == ')')
                listOfBraces.removeAt(listOfBraces.size - 1)
            expression += string[parsedIndex++]
        }
        if (listOfBraces.size != 0) return Pair("", "")
    } else {
        val stringParts = string.split(" ", limit = 2)
        expression = if (stringParts[0].contains(")")) stringParts[0].substring(0, string.indexOf(')')).trim()
        else stringParts[0]
    }
    return Pair(expression, string.substring(expression.length).trimStart())
}

fun evalLambdaExpression(env: Environment, string: String): Pair<Any, String>? {
    val paramsList = identifyAndReturnExpression(string)
    if (!paramsList.first.startsWith("(")) return null

    val parameters = mutableListOf<String>()
    var paramsString = paramsList.first.substring(1, paramsList.first.length)
    while (paramsString != ")") {
        val param = identifyAndReturnExpression(paramsString)
        parameters.add(param.first)
        paramsString = param.second
    }

    val body = identifyAndReturnExpression(paramsList.second)

    if (body.second.trimStart().startsWith(")"))
        return Pair(
            Procedure(*parameters.toTypedArray(), body = body.first),
            body.second.trimStart().substring(1)
        )

    return null
}

fun evalQuoteExpression(string: String): Pair<String, String>? {
    val expLiteral = identifyAndReturnExpression(string)
    if (expLiteral.first == "" || expLiteral.second == "" || !expLiteral.second.trimStart().startsWith(")")) return null

    return Pair(expLiteral.first, expLiteral.second.trimStart().substring(1))
}

fun evalBeginExpression(env: Environment, string: String): Pair<Any, String>? {
    var exp = identifyAndReturnExpression(string)
    var pair: Pair<Any, String>? = null
    while (exp.first != "" && exp.second != "" && exp.second.trimStart() != ")") {
        var pair = evalExpression(env, exp.first)
        exp = identifyAndReturnExpression(exp.second)
    }
    pair = evalExpression(env, exp.first)
    return if (pair != null && exp.second.trimStart().startsWith(")"))
        Pair(pair.first, exp.second.trimStart().substring(1))
    else null
}