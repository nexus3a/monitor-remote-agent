package com.monitor.parser.osprocinfo;

import com.monitor.parser.ConsoleParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindowsProcessesParser implements ConsoleParser {
    
    private final static String RAGENT_PROC_NAME = "ragent";
    private final static String RMNGR_PROC_NAME = "rmngr";
    private final static String RPHOST_PROC_NAME = "rphost";
    
    private final List<ProcessInfo> processes = new ArrayList<>();

    public static class ProcessInfo {

        private final String name;
        private String parentPid;
        private final String pid;
        private List<NetworkInfo> network;
        private final List<ProcessInfo> children = new ArrayList<>();

        ProcessInfo(String name, String pid, String parentId) {
            this.name = name;
            this.parentPid = parentId;
            this.pid = pid;
            this.network = new ArrayList<>();
        }

        public void setParentPid(String parentPid) {
            this.parentPid = parentPid;
        }

        public void setNetwork(List<NetworkInfo> network) {
            this.network = network;
        }

        public String getName() {
            return name;
        }

        public String getParentPid() {
            return parentPid;
        }

        public String getPid() {
            return pid;
        }

        public List<NetworkInfo> getNetwork() {
            return network;
        }

        public List<ProcessInfo> getChildren() {
            return children;
        }
        
        public void addNetworkInfo(NetworkInfo networkInfo) {
            network.add(networkInfo);
        }

    }

    public static class NetworkInfo {

        private final String localIp;
        private final String localPort;
        private final String externalIp;
        private final String externalPort;

        NetworkInfo(String localIp, String localPort, String externalIp, String externalPort) {
            this.localIp = localIp;
            this.localPort = localPort;
            this.externalIp = externalIp;
            this.externalPort = externalPort;
        }

        public String getLocalIp() {
            return localIp;
        }

        public String getLocalPort() {
            return localPort;
        }

        public String getExternalIp() {
            return externalIp;
        }

        public String getExternalPort() {
            return externalPort;
        }
    }

    @Override
    public List<ProcessInfo> parse() throws IOException, InterruptedException {
        getPslistRawData();
        bindNetwork();
        findParentPids();
        deleteRphostNetwork();
        buildProcessesTree();

        return processes;
    }

    /*
    private void getTasklistRawData() throws IOException, InterruptedException {
        
        Process tasklistProcess = Runtime.getRuntime().exec(String.format(
                "cmd.exe /c tasklist | findstr \"%s %s %s\"",
                RAGENT_PROC_NAME, RMNGR_PROC_NAME, RPHOST_PROC_NAME));
        
        try (BufferedReader tasklistReader = new BufferedReader(new InputStreamReader(tasklistProcess.getInputStream()))) {
            String tasklistRecord;
            while ((tasklistRecord = tasklistReader.readLine()) != null) {
                String[] parts = tasklistRecord.trim().split("\\s+");
                if (parts.length > 1) {
                    String name = parts[0].substring(0, parts[0].lastIndexOf("."));
                    String pid = parts[1];
                    processes.add(new ProcessInfo(name, pid, null));
                }
            }
        }
        tasklistProcess.waitFor();
    }
    */
    
    private void getPslistRawData() throws IOException, InterruptedException {
        
        Process pslistProcess = Runtime.getRuntime().exec(String.format(
                "cmd.exe /c utils\\pslist.exe -t | findstr \"%s %s %s\"",
                RAGENT_PROC_NAME, RMNGR_PROC_NAME, RPHOST_PROC_NAME));
        
        ArrayList<String[]> parentPids = new ArrayList<>(); // { shift, pid, parentPid }
        
        try (BufferedReader pslistReader = new BufferedReader(new InputStreamReader(pslistProcess.getInputStream()))) {
            String parentPid;
            String tasklistRecord;
            while ((tasklistRecord = pslistReader.readLine()) != null) {
                String[] parts = tasklistRecord.trim().split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                
                String name = parts[0];
                String pid = parts[1];
                String shift = tasklistRecord.substring(0, tasklistRecord.indexOf(name));
                int shiftLength = shift.length();
                
                int topShiftLength = parentPids.isEmpty() ? -1 : parentPids.getLast()[0].length(); // [0] == .shift
                while (topShiftLength > shiftLength) {
                    parentPids.removeLast();
                    topShiftLength = parentPids.isEmpty() ? -1 : parentPids.getLast()[0].length(); // [0] == .shift
                }

                if (topShiftLength < shiftLength) {
                    parentPid = parentPids.isEmpty() ? null : parentPids.getLast()[1]; // [1] == .pid
                    parentPids.add(new String[] { shift, pid, parentPid });
                }
                else { // topShiftLength == shiftLength
                    String[] lastRecord = parentPids.getLast();
                    lastRecord[1] = pid; // [1] == .pid
                    parentPid = lastRecord[2]; // [2] == .parentPid
                }
                
                processes.add(new ProcessInfo(name, pid, parentPid));
            }
        }
        pslistProcess.waitFor();
        
        // заглушка; привязка rphost будет делаться по netstat
        //
        for (ProcessInfo procInfo : processes) {
            if (RPHOST_PROC_NAME.equals(procInfo.name)) {
                procInfo.parentPid = null; 
            }
        }
    }

    private void bindNetwork() throws IOException, InterruptedException {
        if (processes.isEmpty()) {
            return;
        }
        
        HashMap<String, ProcessInfo> procMap = new HashMap<>();
        String pids = "";
        for (ProcessInfo procInfo : processes) {
            String pid = procInfo.getPid();
            pids = pids + (pids.isEmpty() ? "" : " ") + pid;
            procMap.put(pid, procInfo);
        }
        Process netstatProcess = Runtime.getRuntime().exec("cmd.exe /c netstat /a /n /o | findstr \"" + pids + "\"");
        
        try (BufferedReader netstatReader = new BufferedReader(new InputStreamReader(netstatProcess.getInputStream()))) {
            String netstatRecord;
            while ((netstatRecord = netstatReader.readLine()) != null) {
                String[] partsNetstat = netstatRecord.trim().split("\\s+");
                if (partsNetstat.length <= 4) {
                    continue;
                }
                String protocol = partsNetstat[0].toLowerCase();
                if (!"tcp".equals(protocol) && !"tcpv6".equals(protocol)) {
                    continue;
                }

                String ipAddress = partsNetstat[1];
                int portPlace = ipAddress.lastIndexOf(':');
                String localAddress = ipAddress.substring(0, portPlace);
                String localPort = ipAddress.substring(portPlace + 1, ipAddress.length());

                ipAddress = partsNetstat[2];
                portPlace = ipAddress.lastIndexOf(':');
                String externalAddress = ipAddress.substring(0, portPlace);
                String externalPort = ipAddress.substring(portPlace + 1, ipAddress.length());

                String pid = partsNetstat[4];
                NetworkInfo networkInfo = new NetworkInfo(localAddress, localPort, externalAddress, externalPort);
                procMap.get(pid).addNetworkInfo(networkInfo);
            }
        }
        netstatProcess.waitFor();
    }

    private void findParentPids() {
        List<String> portList = new ArrayList<>();

        for (ProcessInfo local : processes) {
            if (!RMNGR_PROC_NAME.equals(local.getName())) {
                continue;
            }
            String port = local.getNetwork().get(0).getLocalPort();
            portList.add(port);
            
            if (local.getParentPid() != null) {
                continue;
            }

            for (ProcessInfo external : processes) {
                if (!RAGENT_PROC_NAME.equals(external.getName())) {
                    continue;
                }
                boolean isPort = false;
                for (int i = 0; i <= external.getNetwork().size() - 1; i++) {
                    if (external.getNetwork().get(i).getExternalPort().equals(port)) {
                        isPort = true;
                        local.setParentPid(external.getPid());
                        break;
                    }
                }

                if (isPort) {
                    break;
                }
            }
        }

        for (ProcessInfo local : processes) {
            if (!RPHOST_PROC_NAME.equals(local.getName()) || local.getParentPid() != null) {
                continue;
            }
            String regPort = "";
            boolean isRegPort = false;
            for (int i = 0; i <= local.getNetwork().size() - 1; i++) {
                for (String port : portList) {
                    if (local.getNetwork().get(i).getExternalPort().equals(port)) {
                        isRegPort = true;
                        regPort = port;
                        break;
                    }
                }

                if (isRegPort) {
                    break;
                }
            }

            for (ProcessInfo external : processes) {
                if (RMNGR_PROC_NAME.equals(external.getName())) {
                    if (regPort.equals(external.getNetwork().get(0).getLocalPort())) {
                        local.setParentPid(external.getPid());
                    }
                }
            }
        }
    }

    private void deleteRphostNetwork() {
        for (ProcessInfo process : processes) {
            if (RPHOST_PROC_NAME.equals(process.getName())) {
                process.setNetwork(new ArrayList<>());
            }
        }
    }

    private void buildProcessesTree() {
        Map<String, ProcessInfo> processMap = new HashMap<>();
        for (ProcessInfo process : processes) {
            processMap.put(process.getPid(), process);
        }

        List<ProcessInfo> rootProcesses = new ArrayList<>();
        for (ProcessInfo process : processes) {
            if (process.getParentPid() == null || !processMap.containsKey(process.getParentPid())) {
                rootProcesses.add(process);
            }
            else {
                ProcessInfo parent = processMap.get(process.getParentPid());
                parent.getChildren().add(process);
            }
        }

        processes.clear();
        processes.addAll(rootProcesses);
    }
}
