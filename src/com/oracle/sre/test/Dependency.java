package com.oracle.sre.test;

import java.util.ArrayList;
import java.util.List;


public class Dependency {

	  protected String moduleName;
      protected  List<Dependency> dependencyList;

      public Dependency(String moduleName) {
          this.moduleName = moduleName;
          dependencyList = new ArrayList<>();
      }
}
