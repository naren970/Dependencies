package com.oracle.sre.test;

import java.util.HashMap;

import com.oracle.sre.test.Dependency;


public class DependencyController {

    protected HashMap<String,Integer> refCount;
    protected String installAgent;
    protected Dependency dependency;
    
    public DependencyController(Dependency dependency) {
        this.dependency = dependency;
        this.installAgent = null;
        this.refCount = new HashMap();
        

    }
    
    public boolean anyRef() {
    	for(String from: refCount.keySet()) {
    		if(refCount.get(from) > 0) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public int totalRefExcept(String str) {
    	int cnt = 0;
    	for(String from: refCount.keySet()) {
    		if(!str.equals(from)) {
    			cnt += refCount.get(from);
    		}
    		
    	}
    	return cnt;
    }
    
    public int totalRef() {
    	int cnt = 0;
    	for(String from: refCount.keySet()) {
    		cnt += refCount.get(from);
    	}
    	return cnt;
    }
    
    public boolean hasRefFrom(String from) {
    	if(refCount.containsKey(from) && refCount.get(from) > 0) {
    		return true;
    	}
    	
    	return false;
    }
    
    public int addRefFrom(String from) {
    	Integer i;
    	if(refCount.containsKey(from))
    	{
    		i = refCount.get(from);
    		
    	}
    	else {
    		i = new Integer(0);
    		
    		
    	}
    	i++;
    	if(i >= 0) {
    		refCount.put(from, i);
    	}
    	
    	return i;
    }
    
    public int removeRefFrom(String from) {
    	Integer i;
    	if(hasRefFrom(from) == false) {
    		return 0;
    	}
    	i = refCount.get(from);
    	i--;
    	
    	refCount.put(from, i);
    	
    	return i;
    }

}
