package compiler

enum class Lex {
    NONE, NAME, NUM,
    MODULE, IMPORT, BEGIN, END, CONST, VAR,
    WHILE, DO, IF, THEN, ELSIF, ELSE,
    MULT, DIV, MOD, PLUS, MINUS,
    EQ, NE, LT, LE, GT, GE,
    DOT, COMMA, COLON, SEMI, ASS, LPAR, RPAR, EOT
}