package com.databricks.jsonnetjvm.truffle;

import com.databricks.jsonnetjvm.runtime.JArray;
import com.databricks.jsonnetjvm.runtime.JFunction;
import com.databricks.jsonnetjvm.runtime.JNull;
import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem({double.class, boolean.class, String.class, JArray.class, JFunction.class, JNull.class})
public class JsonnetTypes {}
