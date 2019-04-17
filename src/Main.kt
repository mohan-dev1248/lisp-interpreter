import java.io.*

val regex ="^(-?[1-9][0-9]*(([.][0-9]+)?([eE][+-]?[0-9]+)?)?)|^(-?0[.][0-9]+([eE][+-]?[0-9]+)?)|^(0)".toRegex()

fun main(args: Array<String>){
    val text = File("./input.txt").readText()
    println(evalExpression(text))
}

fun evalNumber(string: String): Pair<Double,String>? {
    val num = regex.find(string)?.value
    if(num !=null){
        if(string.length>num.length) {
            if (string[num.length] == ' ' || string[num.length] == ')')
                return Pair(num.toDouble(),string.subSequence(num.length,string.length).toString())
            return null
        }
        return  Pair(num.toDouble(),string.subSequence(num.length,string.length).toString())
    }
    return null
}

fun evalExpression(string: String): Pair<Any,String>?{
    val operators = arrayListOf<Char>('+','-','*','/')

    val pair = evalNumber(string)
    if (pair!=null) return pair

    var current = string.trimStart()
    if(current[0]!='(') return null

    current = current.substring(1).trimStart()
    if(!operators.contains(current[0])) return null

    val operator = current[0]
    current = current.substring(1).trimStart()

    val exprPair = evalExpression(current)
    if(exprPair == null) return null
    var expValue = exprPair.first as Double
    current = exprPair.second.trimStart()

    while(!current.startsWith(')')){
        val operand = evalExpression(current)
        if(operand == null) return null
        try {
            when (operator) {
                '+' -> expValue += operand.first as Double
                '-' -> expValue -= operand.first as Double
                '*' -> expValue *= operand.first as Double
                '/' -> expValue /= operand.first as Double
            }
        }catch (e: NumberFormatException){
            println(e.stackTrace)
            return null
        }
        current = operand.second.trimStart()
    }

    return Pair(expValue, current.substring(1))
}