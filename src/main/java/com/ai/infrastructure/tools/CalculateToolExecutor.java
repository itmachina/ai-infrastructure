package com.ai.infrastructure.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计算工具执行器
 * 支持基本数学运算：加减乘除、比大小等
 */
public class CalculateToolExecutor implements ToolExecutor {
    @Override
    public String execute(String task) {
        try {
            // 移除前缀"Calculate"或"计算"
            String expression = task.replaceFirst("^(Calculate|calculate|计算)\\s*", "").trim();
            
            // 处理不同的计算类型
            if (isComparisonExpression(expression)) {
                return handleComparison(expression);
            } else if (isArithmeticExpression(expression)) {
                return handleArithmetic(expression);
            } else {
                return "Error: Unsupported calculation type - " + expression;
            }
        } catch (Exception e) {
            return "Error: Failed to parse calculation - " + e.getMessage();
        }
    }
    
    /**
     * 判断是否为比较表达式
     * @param expression 表达式
     * @return 是否为比较表达式
     */
    private boolean isComparisonExpression(String expression) {
        return expression.matches(".*[<>]=?\\s*.*|.*==\\s*.*|.*!=\\s*.*");
    }
    
    /**
     * 判断是否为算术表达式
     * @param expression 表达式
     * @return 是否为算术表达式
     */
    private boolean isArithmeticExpression(String expression) {
        return expression.matches(".*[+\\-*/^()].*");
    }
    
    /**
     * 处理比较表达式
     * @param expression 表达式
     * @return 比较结果
     */
    private String handleComparison(String expression) {
        // 支持的比较操作符: >, <, >=, <=, ==, !=
        Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*(>|<|>=|<=|==|!=)\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(expression);
        
        if (matcher.matches()) {
            double left = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            
            boolean result;
            switch (operator) {
                case ">":
                    result = left > right;
                    break;
                case "<":
                    result = left < right;
                    break;
                case ">=":
                    result = left >= right;
                    break;
                case "<=":
                    result = left <= right;
                    break;
                case "==":
                    result = left == right;
                    break;
                case "!=":
                    result = left != right;
                    break;
                default:
                    return "Error: Unsupported comparison operator - " + operator;
            }
            
            return String.format("%s %s %s = %s", matcher.group(1), operator, matcher.group(3), result);
        }
        
        return "Error: Invalid comparison expression - " + expression;
    }
    
    /**
     * 处理算术表达式
     * @param expression 表达式
     * @return 计算结果
     */
    private String handleArithmetic(String expression) {
        try {
            // 移除空格
            expression = expression.replaceAll("\\s+", "");
            
            // 首先检查是否包含括号，如果包含则先处理括号内的表达式
            if (expression.contains("(")) {
                return handleParenthesesExpression(expression);
            }
            
            // 处理带有指数运算的二元运算
            Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/^])\\s*(-?\\d+(?:\\.\\d+)?)");
            Matcher matcher = pattern.matcher(expression);
            
            if (matcher.matches()) {
                double left = Double.parseDouble(matcher.group(1));
                String operator = matcher.group(2);
                double right = Double.parseDouble(matcher.group(3));
                
                double result;
                switch (operator) {
                    case "+":
                        result = left + right;
                        break;
                    case "-":
                        result = left - right;
                        break;
                    case "*":
                        result = left * right;
                        break;
                    case "/":
                        if (right == 0) {
                            return "Error: Division by zero";
                        }
                        result = left / right;
                        break;
                    case "^":
                        result = Math.pow(left, right);
                        break;
                    default:
                        return "Error: Unsupported arithmetic operator - " + operator;
                }
                
                // 格式化结果，如果是整数则不显示小数点
                if (result == (long) result) {
                    return String.format("%s %s %s = %d", matcher.group(1), operator, matcher.group(3), (long) result);
                } else {
                    return String.format("%s %s %s = %.6f", matcher.group(1), operator, matcher.group(3), result);
                }
            }
            
            // 如果简单模式不匹配，尝试处理更复杂的表达式
            return evaluateComplexExpression(expression);
        } catch (NumberFormatException e) {
            return "Error: Invalid number format in expression - " + expression;
        } catch (Exception e) {
            return "Error: Failed to evaluate arithmetic expression - " + e.getMessage();
        }
    }
    
    /**
     * 处理包含括号的表达式
     * @param expression 表达式
     * @return 计算结果
     */
    private String handleParenthesesExpression(String expression) {
        try {
            // 找到最内层的括号对
            int lastOpenParen = expression.lastIndexOf('(');
            if (lastOpenParen == -1) {
                return "Error: Mismatched parentheses";
            }
            
            int closeParen = expression.indexOf(')', lastOpenParen);
            if (closeParen == -1) {
                return "Error: Mismatched parentheses";
            }
            
            // 提取括号内的表达式
            String innerExpression = expression.substring(lastOpenParen + 1, closeParen);
            
            // 计算括号内的表达式
            String innerResult = handleArithmetic(innerExpression);
            
            // 检查计算结果是否包含错误
            if (innerResult.startsWith("Error:")) {
                return innerResult;
            }
            
            // 提取计算结果（去除前面的表达式部分）
            String resultValue = extractResultValue(innerResult);
            
            // 替换原表达式中的括号部分
            String newExpression = expression.substring(0, lastOpenParen) + resultValue + expression.substring(closeParen + 1);
            
            // 递归处理替换后的表达式
            return handleArithmetic(newExpression);
        } catch (Exception e) {
            return "Error: Failed to handle parentheses expression - " + e.getMessage();
        }
    }
    
    /**
     * 从计算结果中提取数值
     * @param result 计算结果
     * @return 数值字符串
     */
    private String extractResultValue(String result) {
        // 查找等号后面的结果
        int equalsIndex = result.lastIndexOf(" = ");
        if (equalsIndex != -1) {
            return result.substring(equalsIndex + 3); // 跳过" = "
        }
        
        // 如果没有找到等号，返回整个结果
        return result;
    }
    
    /**
     * 计算复杂表达式
     * 支持括号、指数运算等复杂操作符
     * @param expression 表达式
     * @return 计算结果
     */
    private String evaluateComplexExpression(String expression) {
        try {
            // 移除空格
            expression = expression.replaceAll("\\s+", "");
            
            // 处理空表达式
            if (expression.isEmpty()) {
                return "Error: Empty expression";
            }
            
            // 使用递归下降解析器处理复杂表达式
            return parseExpression(expression, 0).result;
        } catch (Exception e) {
            return "Error: Failed to evaluate complex expression - " + e.getMessage();
        }
    }
    
    /**
     * 表达式解析结果
     */
    private static class ParseResult {
        final String result;
        final int nextIndex;
        
        ParseResult(String result, int nextIndex) {
            this.result = result;
            this.nextIndex = nextIndex;
        }
    }
    
    /**
     * 解析表达式（处理加法和减法）
     * @param expression 表达式
     * @param startIndex 起始索引
     * @return 解析结果
     */
    private ParseResult parseExpression(String expression, int startIndex) {
        ParseResult leftResult = parseTerm(expression, startIndex);
        if (leftResult.result.startsWith("Error:")) {
            return leftResult;
        }
        
        int index = leftResult.nextIndex;
        while (index < expression.length()) {
            char operator = expression.charAt(index);
            if (operator == '+' || operator == '-') {
                ParseResult rightResult = parseTerm(expression, index + 1);
                if (rightResult.result.startsWith("Error:")) {
                    return rightResult;
                }
                
                // 计算结果
                String calculationResult = calculateBinaryOperation(
                    extractValueFromResult(leftResult.result), 
                    String.valueOf(operator), 
                    extractValueFromResult(rightResult.result)
                );
                
                if (calculationResult.startsWith("Error:")) {
                    return new ParseResult(calculationResult, rightResult.nextIndex);
                }
                
                leftResult = new ParseResult(calculationResult, rightResult.nextIndex);
                index = leftResult.nextIndex;
            } else {
                break;
            }
        }
        
        return leftResult;
    }
    
    /**
     * 解析项（处理乘法、除法和指数）
     * @param expression 表达式
     * @param startIndex 起始索引
     * @return 解析结果
     */
    private ParseResult parseTerm(String expression, int startIndex) {
        // 首先处理指数运算（右结合）
        ParseResult leftResult = parseFactorForExponent(expression, startIndex);
        if (leftResult.result.startsWith("Error:")) {
            return leftResult;
        }
        
        int index = leftResult.nextIndex;
        while (index < expression.length()) {
            char operator = expression.charAt(index);
            if (operator == '*' || operator == '/') {
                ParseResult rightResult = parseFactorForExponent(expression, index + 1);
                if (rightResult.result.startsWith("Error:")) {
                    return rightResult;
                }
                
                // 计算结果
                String calculationResult = calculateBinaryOperation(
                    extractValueFromResult(leftResult.result), 
                    String.valueOf(operator), 
                    extractValueFromResult(rightResult.result)
                );
                
                if (calculationResult.startsWith("Error:")) {
                    return new ParseResult(calculationResult, rightResult.nextIndex);
                }
                
                leftResult = new ParseResult(calculationResult, rightResult.nextIndex);
                index = leftResult.nextIndex;
            } else {
                break;
            }
        }
        
        return leftResult;
    }
    
    /**
     * 解析因子（处理数字和括号，用于指数运算）
     * @param expression 表达式
     * @param startIndex 起始索引
     * @return 解析结果
     */
    private ParseResult parseFactorForExponent(String expression, int startIndex) {
        ParseResult leftResult = parseFactor(expression, startIndex);
        if (leftResult.result.startsWith("Error:")) {
            return leftResult;
        }
        
        int index = leftResult.nextIndex;
        // 指数运算是右结合的，所以需要特殊处理
        if (index < expression.length() && expression.charAt(index) == '^') {
            ParseResult rightResult = parseFactorForExponent(expression, index + 1);
            if (rightResult.result.startsWith("Error:")) {
                return rightResult;
            }
            
            // 计算指数运算结果
            String calculationResult = calculateBinaryOperation(
                extractValueFromResult(leftResult.result), 
                "^", 
                extractValueFromResult(rightResult.result)
            );
            
            if (calculationResult.startsWith("Error:")) {
                return new ParseResult(calculationResult, rightResult.nextIndex);
            }
            
            return new ParseResult(calculationResult, rightResult.nextIndex);
        }
        
        return leftResult;
    }
    
    /**
     * 解析因子（处理数字、括号和指数）
     * @param expression 表达式
     * @param startIndex 起始索引
     * @return 解析结果
     */
    private ParseResult parseFactor(String expression, int startIndex) {
        if (startIndex >= expression.length()) {
            return new ParseResult("Error: Unexpected end of expression", startIndex);
        }
        
        char ch = expression.charAt(startIndex);
        
        // 处理负号
        if (ch == '-') {
            ParseResult result = parseFactor(expression, startIndex + 1);
            if (result.result.startsWith("Error:")) {
                return result;
            }
            
            double value = Double.parseDouble(extractValueFromResult(result.result));
            String negatedValue = String.valueOf(-value);
            return new ParseResult(formatResult(negatedValue, negatedValue), result.nextIndex);
        }
        
        // 处理正号
        if (ch == '+') {
            return parseFactor(expression, startIndex + 1);
        }
        
        // 处理括号
        if (ch == '(') {
            ParseResult result = parseExpression(expression, startIndex + 1);
            if (result.result.startsWith("Error:")) {
                return result;
            }
            
            // 检查右括号
            if (result.nextIndex >= expression.length() || expression.charAt(result.nextIndex) != ')') {
                return new ParseResult("Error: Missing closing parenthesis", result.nextIndex);
            }
            
            return new ParseResult(result.result, result.nextIndex + 1);
        }
        
        // 处理数字
        if (Character.isDigit(ch) || ch == '.') {
            int index = startIndex;
            while (index < expression.length() && 
                   (Character.isDigit(expression.charAt(index)) || expression.charAt(index) == '.')) {
                index++;
            }
            
            String number = expression.substring(startIndex, index);
            return new ParseResult(formatResult(number, number), index);
        }
        
        return new ParseResult("Error: Unexpected character: " + ch, startIndex);
    }
    
    /**
     * 执行二元运算
     * @param left 左操作数
     * @param operator 运算符
     * @param right 右操作数
     * @return 运算结果
     */
    private String calculateBinaryOperation(String left, String operator, String right) {
        try {
            double leftValue = Double.parseDouble(left);
            double rightValue = Double.parseDouble(right);
            
            double result;
            switch (operator) {
                case "+":
                    result = leftValue + rightValue;
                    break;
                case "-":
                    result = leftValue - rightValue;
                    break;
                case "*":
                    result = leftValue * rightValue;
                    break;
                case "/":
                    if (rightValue == 0) {
                        return "Error: Division by zero";
                    }
                    result = leftValue / rightValue;
                    break;
                case "^":
                    result = Math.pow(leftValue, rightValue);
                    break;
                default:
                    return "Error: Unsupported operator: " + operator;
            }
            
            return formatResult(String.valueOf(result), left + " " + operator + " " + right);
        } catch (NumberFormatException e) {
            return "Error: Invalid number format";
        }
    }
    
    /**
     * 从计算结果中提取数值
     * @param result 计算结果
     * @return 数值字符串
     */
    private String extractValueFromResult(String result) {
        // 如果结果包含" = "，则提取等号后面的部分
        int equalsIndex = result.indexOf(" = ");
        if (equalsIndex != -1) {
            return result.substring(equalsIndex + 3);
        }
        return result;
    }
    
    /**
     * 格式化结果
     * @param value 数值
     * @param expression 表达式
     * @return 格式化结果
     */
    private String formatResult(String value, String expression) {
        try {
            double num = Double.parseDouble(value);
            if (num == (long) num) {
                return String.format("%s = %d", expression, (long) num);
            } else {
                // 格式化为6位小数，但去掉尾随的0
                return String.format("%s = %.6f", expression, num).replaceAll("0*$", "").replaceAll("\\.$", "");
            }
        } catch (NumberFormatException e) {
            return expression + " = " + value;
        }
    }
}