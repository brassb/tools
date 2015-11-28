import java.util.ArrayList;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.*;

public class RunScriptOnAllNodes
{
  private static String remoteUser = null;

  private static class RemoteRunnerThread extends Thread
  {
    private static final Integer lockObject  = new Integer(0);
    public  static int           threadCount = 0;

    private String      hostname;
    private String      scriptName;
    private FileWriter  outputFileWriter;

    public String      getScriptName()        { return scriptName;       }
    public String      getHostname()          { return hostname;         }
    public FileWriter  getOutputFileWriter()  { return outputFileWriter; }

    public void   setScriptName(       String     s ) { scriptName       = s; }
    public void   setHostname(         String     s ) { hostname         = s; }
    public void   setOutputFileWriter( FileWriter w ) { outputFileWriter = w; }

    public void run()
    {
      try
      {
        java.util.Date now = new java.util.Date();
        long epochMillis = now.getTime();
        String remoteFilename = "/tmp/runScript_" + epochMillis;

        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("scp -p " + scriptName + " " + remoteUser + "@" + hostname + ":" + remoteFilename);
        int exitCode = p.waitFor();

        p = rt.exec("ssh " + remoteUser + "@" + hostname + " " + remoteFilename);
        exitCode = p.waitFor();

        synchronized(lockObject)
        {
          System.out.println(     "########## BEGIN OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########"   );
          outputFileWriter.write( "########## BEGIN OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########\n" );

          BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
          while (br.ready())
          {
            String line = br.readLine();
            System.out.println(line);
            outputFileWriter.write(line + "\n");
          }
          br.close();

          br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
          while (br.ready())
          {
            String line = br.readLine();
            System.out.println(line);
            outputFileWriter.write(line + "\n");
          }
          br.close();

          System.out.println(     "########## END   OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########"   );
          outputFileWriter.write( "########## END   OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########\n" );
          System.out.println();
          outputFileWriter.write("\n");
          outputFileWriter.flush();
          threadCount--;
        }

        p = rt.exec("ssh " + remoteUser + "@" + hostname + " /bin/rm -f " + remoteFilename);
        exitCode = p.waitFor();
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args)
  {
    ResourceBundle configProps = null;
    Pattern blankLinePattern   = Pattern.compile("^\\s*$");
    Matcher blankLineMatcher   = null;

    ArrayList<String> hostnameList = new ArrayList<String>();

    try
    {
      configProps = ResourceBundle.getBundle("remote_commands");

      BufferedReader br = new BufferedReader(new FileReader(configProps.getString("host_name_list_file")));
      while (br.ready())
      {
        String line = br.readLine();
        blankLineMatcher = blankLinePattern.matcher(line);
        if (!line.startsWith("#") && !blankLineMatcher.matches())
        {
          hostnameList.add(line);
        }
      }
      br.close();

      br = new BufferedReader(new FileReader(configProps.getString("remote_user_file")));
      remoteUser = br.readLine();
      br.close();
      remoteUser = remoteUser.trim();
      System.out.println("remoteUser = '" + remoteUser + "'");
    }
    catch (IOException e)
    {
      e.printStackTrace();
      System.exit(1);
    }

    String scriptName = null;

    if (args.length != 1)
    {
      System.out.println("\n  Usage: java RunScriptOnAllNodes <script_filename>\n");
      System.exit(1);
    }

    scriptName = args[0];

    try
    {
      FileWriter fw = new FileWriter(configProps.getString("output_file"));

      RemoteRunnerThread.threadCount = hostnameList.size();
      for (int i=0; i<hostnameList.size(); i++)
      {
        String hostname = (String) hostnameList.get(i);
        RemoteRunnerThread rrt = new RemoteRunnerThread();
        rrt.setName(hostname);
        rrt.setHostname(hostname);
        rrt.setScriptName(scriptName);
        rrt.setOutputFileWriter(fw);
        rrt.start();
      }

      while (RemoteRunnerThread.threadCount > 0)
      {
        try { Thread.sleep(10); } catch (InterruptedException e) {}
      }

      fw.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
}
