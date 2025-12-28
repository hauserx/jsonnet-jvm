grammar Jsonnet;

// Parser Rules

jsonnet
    : expr EOF
    ;

expr
    : 'null'                                            # Null
    | 'true'                                            # True
    | 'false'                                           # False
    | 'self'                                            # Self
    | '$'                                               # Dollar
    | STRING                                            # StringLit
    | NUMBER                                            # NumberLit
    | '{' objinside? '}'                                # Object
    | '[' (expr (',' expr)* ','?)? ']'                  # Array
    | '[' expr ','? 'for' id 'in' expr compspec* ']'    # ArrayComp
    | 'super' '[' expr ']'                              # SuperIndex
    | 'super' '.' id                                    # SuperField
    | expr '(' args? ')'                                # Call
    | expr '[' expr? (':' expr? (':' expr?)?)? ']'      # IndexOrSlice
    | expr '.' id                                       # FieldAccess
    | '-' expr                                          # UnaryMinus
    | '+' expr                                          # UnaryPlus
    | '!' expr                                          # UnaryNot
    | '~' expr                                          # UnaryBitNot
    | expr ('*' | '/' | '%') expr                       # Multiplicative
    | expr ('+' | '-') expr                             # Additive
    | expr ('<<' | '>>') expr                           # Shift
    | expr ('<' | '>' | '<=' | '>=' | 'in') expr        # Relational
    | expr ('==' | '!=') expr                           # Equality
    | expr '&' expr                                     # BitwiseAnd
    | expr '^' expr                                     # BitwiseXor
    | expr '|' expr                                     # BitwiseOr
    | expr '&&' expr                                    # LogicalAnd
    | expr '||' expr                                    # LogicalOr
    | 'local' bind (',' bind)* ';' expr                 # LocalVar
    | 'if' expr 'then' expr ('else' expr)?              # IfElse
    | 'function' '(' params? ')' expr                   # Function
    | 'import' STRING                                   # Import
    | 'importstr' STRING                                # ImportStr
    | 'importbin' STRING                                # ImportBin
    | 'error' expr                                      # Error
    | 'assert' expr (':' expr)? ';' expr                # Assert
    | expr '{' objinside? '}'                           # Apply
    | expr 'in' 'super'                                 # InSuper
    | '(' expr ')'                                      # Paren
    | id                                                # Var
    ;

objinside
    : member (',' member)* ','?
    | (objlocal ',')* '[' expr ']' ':' expr ((',' objlocal)* ','? 'for' id 'in' expr compspec*)?
    ;

member
    : objlocal
    | assertStmt
    | field
    ;

field
    : fieldname '+'? h expr
    | fieldname '(' params? ')' h expr
    ;

h : ':' | '::' | ':::';

objlocal
    : 'local' bind
    ;

compspec
    : 'for' id 'in' expr
    | 'if' expr
    ;

fieldname
    : id
    | STRING
    | '[' expr ']'
    ;

assertStmt
    : 'assert' expr (':' expr)?
    ;

bind
    : id '=' expr
    | id '(' params? ')' '=' expr
    ;

args
    : (expr (',' expr)* (',' id '=' expr)* | id '=' expr (',' id '=' expr)*) ','?
    ;

params
    : param (',' param)* ','?
    ;

param
    : id ('=' expr)?
    ;

id : ID;

// Lexer Rules

ASSERT: 'assert';
ELSE: 'else';
ERROR: 'error';
FALSE: 'false';
FOR: 'for';
FUNCTION: 'function';
IF: 'if';
IMPORT: 'import';
IMPORTSTR: 'importstr';
IMPORTBIN: 'importbin';
IN: 'in';
LOCAL: 'local';
NULL: 'null';
TAILSTRICT: 'tailstrict';
THEN: 'then';
SELF: 'self';
SUPER: 'super';
TRUE: 'true';

ID : [_a-zA-Z][_a-zA-Z0-9]* ;

NUMBER
    : '-'? ('0' | [1-9] [0-9]*) ('.' [0-9]+)? ([eE] [+-]? [0-9]+)?
    ;

STRING
    : '"' (ESC | ~["\\])* '"'
    | '\'' (ESC | ~['\\])* '\''
    | '@' '"' (~'"' | '""')* '"'
    | '@' '\'' (~'\'' | '\'\'')* '\''
    | '|||' .*? '|||'
    ;

fragment ESC : '\\' (["\\/bfnrt] | 'u' [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F]) ;

WS : [ \t\r\n]+ -> skip ;

LINE_COMMENT : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
HASH_COMMENT : '#' ~[\r\n]* -> skip ;
