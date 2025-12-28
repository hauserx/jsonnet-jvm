grammar Jsonnet;

jsonnet
    : expr EOF
    ;

expr
    : STRING
    ;

STRING
    : '"' .*? '"'
    ;

WS
    : [ \t\r\n]+ -> skip
    ;