
/**
 * Created By Narendra
 */

package com.oracle.sre.test;

import java.io.*;
import java.util.*;

import com.oracle.sre.test.DependencyController;
import com.oracle.sre.test.Dependency;

public class Driver {


    //Command List
    public final static String COMMAND_END = "END";
    public final static String COMMAND_DEPEND = "DEPEND";
    public final static String COMMAND_INSTALL = "INSTALL";
    public final static String COMMAND_REMOVE = "REMOVE";
    public final static String COMMAND_LIST = "LIST";

    
    //Createing DataStructuer for Dependencies and all
    protected HashMap<String,Dependency> dependencyHashMap;
    protected HashMap<String,DependencyController> installManifest;
    public boolean isDebug = false;


    /**
     * Initilize the HashMap of Dependencies 
     */
    public Driver() {
        dependencyHashMap = new HashMap<>();
        installManifest = new HashMap<>();
    }
    
    /**
     * Validate the Input Line by Checking COMMANDS LIST
     * @param line
     * @return
     */
    protected boolean isValidLine(String line) {
        if(line == null) {
            return false;
        }

        if(
            line.startsWith(COMMAND_END) ||
            line.startsWith(COMMAND_DEPEND) ||
            line.startsWith(COMMAND_INSTALL) ||
            line.startsWith(COMMAND_REMOVE) ||
                    line.startsWith(COMMAND_LIST)

                )
        {
            return true;
        }

        return false;
    }

    /**
     * Check Input the END or NOT
     * @param line
     * @return
     */
    protected  boolean isStopLine(String line) {
        return line != null && line.startsWith(COMMAND_END);
    }

    
   /** Will Print the given string
    * 
    * @param str
    */
    protected  void emit(String str) {
        System.out.print(str);
    }
    
    /**
     * Maintain the Dependencies in HashMap
     * @param moduleName
     * @return
     */
    protected  int processDependencyModule(String moduleName) {

        if(moduleName == null || moduleName.length() == 0) {
            return -1;
        }
        if(!dependencyHashMap.containsKey(moduleName)) {
            Dependency dependency = new Dependency(moduleName);
            dependencyHashMap.put(moduleName,dependency);
        }

        emit("DEPEND " + moduleName + " ");
        return 0;

    }

    
    /**
     * Stack the Dependencies 
     * @param rootModule
     * @param target
     * @return
     */
    protected  boolean findDescendent(String rootModule, String target) {
        if(!dependencyHashMap.containsKey((rootModule))) {
            return false;
        }

        Stack<Dependency> stack = new Stack<>();
        Dependency module = dependencyHashMap.get(rootModule);
        stack.push(module);
        while(stack.size() > 0 ) {
            Dependency mod = (Dependency)stack.pop();
            if(mod.moduleName.equals(target)) {
                return true;
            }

            for(Dependency d : mod.dependencyList) {
                stack.push(d);
            }
        }

        return false;
    }
    
    /**
     * Will Check If dependency already installed or not and maintain it. 
     * @param moduleName
     * @param dependentName
     * @return
     */
    protected  int processDependency(String moduleName, String dependentName) {

        if(!dependencyHashMap.containsKey(moduleName)) {
            return -1;
        }

        Dependency moduleRoot = dependencyHashMap.get(moduleName);

        if(moduleRoot == null) {
            return -1;
        }

        Dependency dependentModuleEntry = dependencyHashMap.get(dependentName);

        if(findDescendent(dependentName,moduleName)) {
             emit(" depends on " + dependentName + " ignoring command.\n");
            return -1;
        }
        if(dependentModuleEntry != null) {

            moduleRoot.dependencyList.add(dependentModuleEntry);
        }
        else {

            dependentModuleEntry = new Dependency(dependentName);
            dependencyHashMap.put(dependentName, dependentModuleEntry);
            moduleRoot.dependencyList.add(dependentModuleEntry);
        }

        emit(dependentName);
        emit(" ");
        return 0;
    }
    
    /**
     * Read DEPEND Command and process and dependencies too.
     * @param line
     * @return
     */
    protected  int processDependLine(String line) {

        StringTokenizer st = new StringTokenizer(line);

        st.nextElement();
        String moduleName = (String)st.nextElement();

        if(moduleName == null || moduleName.length() == 0) {
            return -1;
        }
        if(processDependencyModule(moduleName) < 0) {
            return -1;
        }
        while (st.hasMoreElements()) {
            String dep = (String)st.nextElement();
            int ret;
            ret = processDependency(moduleName,dep);

            if(ret == -1) {
                return -1;
            }

        }

        emit("\n");
        return 0;
    }
    
    /**
     * Remove the Module and its dependences 
     * @param moduleName
     * @param agent
     * @return
     */
    protected  boolean removeModule(String moduleName, String agent) {

        if(!installManifest.containsKey(moduleName)) {
            return false;
        }
        DependencyController dependencyInstance = installManifest.get(moduleName);



        boolean removed = false;
        for(Dependency d : dependencyInstance.dependency.dependencyList) {
            DependencyController dependencyInstance1 = installManifest.get(d.moduleName);
            if(dependencyInstance1 == null) {
                continue;
            }
            removeModule(dependencyInstance1.dependency.moduleName,moduleName);
            
        }

        dependencyInstance.removeRefFrom(agent);
        
        if(dependencyInstance.anyRef() == false) {
            emit(dependencyInstance.dependency.moduleName +"  successfully removed " +  "\n" );
            installManifest.remove(dependencyInstance.dependency.moduleName);
            removed = true;

        }
        return removed;
    }
    
    /**
     * Remove the Entire Dependencies List
     * @param line
     * @return boolean
     */
    protected  boolean processRemoveLine(String line) {

        StringTokenizer st = new StringTokenizer(line);

        st.nextElement(); //EAT REMOVE :)
        String moduleName = (String)st.nextElement();

        if(moduleName == null || moduleName.length() == 0) {
            return false;
        }

        emit("REMOVE " + moduleName + "\n");
        if(!installManifest.containsKey(moduleName)) {

            emit("\t" + moduleName + " is not installed\n");
            return false;
        }

        

        boolean retval;
        if(installManifest.containsKey(moduleName) && installManifest.get(moduleName).totalRefExcept("commandline")  > 0 ) {
            emit("\t" + moduleName + " is still needed " + (isDebug ?  installManifest.get(moduleName).refCount : " ") +  " \n");
            retval = false;
        }
        else {
        	 retval = removeModule(moduleName,"commandline");
        }
        return retval;
    }
    
    /**
     * Will Just Print the All Necessary Dependencies 
     * @param map
     * @return
     */
    public String dump(HashMap<String,Integer> map) {
    	String s = "";
    	Iterator iterator = map.keySet().iterator();

    	while (iterator.hasNext()) {
    	   String key = iterator.next().toString();
    	   Integer value = map.get(key);

    	   s += (key + " " + value);
    	}
    	return s;
    }
    
    /**
     * Install the Module Name 
     * @param module
     * @param agent
     * @return
     */
    protected boolean installModule(String module, String agent) {
        Dependency dependency = dependencyHashMap.get(module);
        DependencyController dependencyInstance = installManifest.get(module);
        
        if(dependency == null) {
        	dependency = new Dependency(module);
        }
        boolean reallyDidIt = false;
       
        if(dependencyInstance == null) {
            dependencyInstance = new DependencyController(dependency);
            reallyDidIt = true;
        }
        dependencyInstance.addRefFrom(agent);
        
        dependencyInstance.installAgent = agent;
        if(isDebug) {
        	emit("\tDEBUG: " + module + " " +  dependencyInstance.refCount + "\n");
        }
        installManifest.put(module, dependencyInstance);

        for(Dependency d: dependency.dependencyList) {

            installModule(d.moduleName, module);

        }
        
        if(reallyDidIt) {
        	emit( module + "  successfully installed " +"\n");
        }
        return reallyDidIt;
    }
    
    /**
     * Process The Entire Installation of Dependencies 
     * @param module
     * @param agent
     * @return
     */
    protected  int processInstall(String module, String agent) {
        emit("INSTALL " + module + "\n");
        if(installManifest.containsKey(module)) {

            /*
            Question: if agent is commandline, does this replace with a force? easy out for now.
             */
            emit("\t" + module + " is already installed \n");
            return 0;
        }

        installModule(module, agent);
        return 0;
    }
    
    /**
     * Process the Entire Dependencies List
     * @param line
     * @param agent
     * @return
     */
    protected int processLineInstallLine(String line, String agent) {
        StringTokenizer st = new StringTokenizer(line);

        st.nextElement(); //EAT DEPEND :)
        String moduleName = (String)st.nextElement();

        if(moduleName == null || moduleName.length() == 0) {
            return -1;
        }

        return processInstall(moduleName,agent);

    }
    
    /**
     * Process the List lines 
     * @param line
     */
    protected void processListLine(String line) {
        emit("LIST\n");
        for(DependencyController dependencyInstance : installManifest.values()) {
            emit("\t" + dependencyInstance.dependency.moduleName + " " + /*dependencyInstance.refCount +*/ "\n");
        }
    }
    
    /**
     * Process the Entire Line of Dependencies
     * @param line
     */
    protected  void processLine(String line) {
        if(line == null || line.length() == 0) {
            
            return;
        }

        if(isStopLine(line)) {
            return;
        }

        if(!isValidLine(line)) {
           
            return;
        }

        if(line.startsWith((COMMAND_DEPEND))) {
            processDependLine(line);
        } else if(line.startsWith(COMMAND_INSTALL)) {
            processLineInstallLine(line,"commandline");
        } else if(line.startsWith(COMMAND_LIST)) {
            processListLine(line);
        }
        else if(line.startsWith(COMMAND_REMOVE)) {
            processRemoveLine(line);
        }


    }
    /**
     * Read the Commands from input stream and process it
     * @param is
     * @return
     */
    public int run(InputStream is) {

        if(is == null) {
            return -1;
        }
        ArrayList<String> lines = new ArrayList<>();

        BufferedReader r1 = new BufferedReader(new InputStreamReader(is));

        boolean bStop = false;
        boolean bErrorState = false;

        while(!bStop) {
            String line;
            try {
                line = r1.readLine();

                if(!isValidLine(line)) {
                    bStop = true;
                    bErrorState = true;
                    break;
                }
                if(isStopLine(line)) {
                    bStop = true;
                    emit("END\n");
                    break;
                }
                processLine(line);

            }
            catch (Exception e) {
                bStop = true;
                e.printStackTrace();
                bErrorState = true;
            }

        }

        if(bErrorState)
        {
            //System.err.println("ABNORMAL EXIT DID NOT MEET END");
            return -1;
        }

       return 0;
    }


    public static void main(String[] args) {
	
    	
    	String inputData = "DEPEND TELNET TCPIP NETCARD\n" + 
    			"DEPEND TCPIP NETCARD\n" + 
    			"DEPEND DNS TCPIP NETCARD\n" + 
    			"DEPEND BROWSER TCPIP HTML\n" + 
    			"INSTALL NETCARD\n" + 
    			"INSTALL TELNET\n"
    			+ "INSTALL foo\n" + 
    			"REMOVE NETCARD\n" + 
    			"INSTALL BROWSER\n" + 
    			"INSTALL DNS\n" + 
    			"LIST\n" + 
    			"REMOVE TELNET\n" + 
    			"REMOVE NETCARD\n" + 
    			"REMOVE DNS\n" + 
    			"REMOVE NETCARD\n" + 
    			"INSTALL NETCARD\n" + 
    			"REMOVE TCPIP\n" + 
    			"REMOVE BROWSER\n" + 
    			"REMOVE TCPIP LIST\n" + 
    			"END";
    	
    	
  
    	 Driver driverObject = new Driver();
         //m.isDebug = false;
         StringBuffer sbf = new StringBuffer(inputData);
         byte[] bytes = sbf.toString().getBytes();
      
         InputStream inputStream = new ByteArrayInputStream(bytes);

         driverObject.run(inputStream);


    }
}

