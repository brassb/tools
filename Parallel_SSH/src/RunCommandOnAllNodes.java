import java.io.*;
import java.util.*;
import java.util.regex.*;

public class RunCommandOnAllNodes
{
  private static String remoteUser = null;

  public static ArrayList<String> cmdList       = new ArrayList<String>();
  public static int               cmdIndex      = 0;
  public static char              escChar       = (char) 27;
  public static char[]            escArr        = { escChar };
  public static String            esc           = new String(escArr);
  public static String            escClearLine  = esc + "[2K";
  public static String            escClearToEol = esc + "[0K";  // same as esc + "[K"
  public static String            escClearDown  = esc + "[0J";  // same as esc + "[J"
  public static String            escMoveUp     = esc + "[A";
  public static String            escMoveDown   = esc + "[B";
  public static String            escMoveRight  = esc + "[C";
  public static String            escMoveLeft   = esc + "[D";

  public static boolean           insertMode   = true;
  public static boolean           doneEditing  = false;

  public static int               xPos         = 0;
  public static int               yPos         = 0;

  private static class RemoteRunnerThread extends Thread
  {
    private static final Integer lockObject  = new Integer(0);
    public  static int           threadCount = 0;

    private String      hostname;
    private String      command;
    private FileWriter  outputFileWriter;

    public String      getCommand()           { return command;          }
    public String      getHostname()          { return hostname;         }
    public FileWriter  getOutputFileWriter()  { return outputFileWriter; }

    public void   setCommand(          String     s ) { command          = s; }
    public void   setHostname(         String     s ) { hostname         = s; }
    public void   setOutputFileWriter( FileWriter w ) { outputFileWriter = w; }

    public void run()
    {
      try
      {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("ssh " + remoteUser + "@" + hostname + " " + command);

        int exitCode = p.waitFor();

        synchronized(lockObject)
        {
          System.out.println(     "########## BEGIN OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########\r" );
          outputFileWriter.write( "########## BEGIN OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########\n" );

          BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
          while (br.ready())
          {
            String line = br.readLine();
            System.out.println(line + "\r");
            outputFileWriter.write(line + "\n");
          }
          br.close();

          br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
          while (br.ready())
          {
            String line = br.readLine();
            System.out.println(line + "\r");
            outputFileWriter.write(line + "\n");
          }
          br.close();

          System.out.println(     "########## END   OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########\r" );
          outputFileWriter.write( "########## END   OUTPUT FOR " + this.getName() + " (exitCode = " + exitCode + ") ##########\n" );
          System.out.println("\r");
          outputFileWriter.write("\n");
          outputFileWriter.flush();
          threadCount--;
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args)
  {
    Pattern blankLinePattern = Pattern.compile("^\\s*$");
    Matcher blankLineMatcher = null;

    int rows = Integer.parseInt(args[0]);
    int cols = Integer.parseInt(args[1]);

    ArrayList<String> hostnameList = new ArrayList<String>();

    try
    {
      ResourceBundle configProps = ResourceBundle.getBundle("remote_commands");

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

      br = new BufferedReader(new FileReader(configProps.getString("remote_user_file")));  // all_nodes_config.txt
      remoteUser = br.readLine();
      br.close();
      remoteUser = remoteUser.trim();
      System.out.println("remoteUser = '" + remoteUser + "'\r");

      br = new BufferedReader(new FileReader(configProps.getString("command_history_file")));
      while (br.ready())
      {
        String line = br.readLine();
        cmdList.add(line);
      }
      br.close();

      System.out.println("Most recently used commands:\r");
      System.out.println("\r");
      for (int i=cmdList.size()-10; i<=cmdList.size()-1; i++)
      {
        if (i >= 0)
        {
          System.out.println(cmdList.get(i) + "\r");
        }
      }
      System.out.println("\r");
      cmdList.add("");

      cmdIndex = cmdList.size() - 1;

      InputStreamReader isr = new InputStreamReader(System.in);
      int key = -1;

      // Big, ugly keystroke-reader loop
      while ((key != 3) && !doneEditing)
      {
        if (isr.ready())
        {
          key = isr.read();
          if (key == 27) // Start of an Escape sequence...
          {
            key = isr.read();
            if (key == 91) // The "[" (open square bracket) character
            {
              key = isr.read();
              if (key == 65) // The "A" character (ESC-[-A sequence comes from the up-arrow key)
              {
                cmdIndex--;
                if (cmdIndex < 0) { cmdIndex = 0; }
                System.out.print(escClearLine + "\r" + cmdList.get(cmdIndex) + escClearToEol + escClearDown + "\r");
                System.out.flush();
                xPos = 0;
                yPos = (((String)cmdList.get(cmdIndex)).length() - 1) / cols;
                while (yPos > 0)
                {
                  System.out.print(escMoveUp);
                  yPos--;
                }
                System.out.flush();
              }
              else if (key == 66) // The "B" character (ESC-[-B sequence comes from the down-arrow key)
              {
                cmdIndex++;
                if (cmdIndex >= cmdList.size()) { cmdIndex = cmdList.size() - 1; }
                System.out.print(escClearLine + "\r" + cmdList.get(cmdIndex) + escClearToEol + escClearDown + "\r");
                System.out.flush();
                xPos = 0;
                yPos = (((String)cmdList.get(cmdIndex)).length() - 1)/ cols;
                while (yPos > 0)
                {
                  System.out.print(escMoveUp);
                  yPos--;
                }
                System.out.flush();
              }
              else if (key == 67) // The "C" character (ESC-[-C sequence comes from the right-arrow key)
              {
                if (((String) cmdList.get(cmdIndex)).length() < cols)
                {
                  if (xPos < ((String) cmdList.get(cmdIndex)).length())
                  {
                    System.out.print(escMoveRight);
                    System.out.flush();
                    xPos++;
                  }
                }
                else
                {
                  int oldPos = yPos * cols + xPos;
                  if (oldPos < ((String)cmdList.get(cmdIndex)).length())
                  {
                    int newPos = oldPos + 1;
                    int yNew = newPos / cols;
                    int xNew = newPos % cols;
                    if (xNew == 0)
                    {
                      System.out.print("\r");
                      System.out.print(escMoveDown);
                      xPos = 0;
                      yPos++;
                    }
                    else
                    {
                      System.out.print(escMoveRight);
                      xPos++;
                    }
                    System.out.flush();
                  }
                }
              }
              else if (key == 68) // The "D" character (ESC-[-D sequence comes from the left-arrow key)
              {
                if (((String) cmdList.get(cmdIndex)).length() < cols)
                {
                  if (xPos > 0)
                  {
                    System.out.print(escMoveLeft);
                    System.out.flush();
                    xPos --;
                  }
                }
                else
                {
                  int oldPos = yPos * cols + xPos;
                  if (oldPos > 0)
                  {
                    int newPos = oldPos - 1;
                    int yNew = newPos / cols;
                    int xNew = newPos % cols;
                    if (xNew == cols - 1)
                    {
                      System.out.print("\r");
                      for (int i=0; i<cols; i++)
                      {
                        System.out.print(escMoveRight);
                      }
                      System.out.print(escMoveUp);
                      xPos = cols - 1;
                      yPos--;
                    }
                    else
                    {
                      System.out.print(escMoveLeft);
                      xPos--;
                    }
                    System.out.flush();
                  }
                }
              }
            }
          }
          else if (key == 1) // CTRL-A (move cursor to beginning of command string)
          {
            System.out.print("\r");
            System.out.flush();
            xPos = 0;

            if (((String)cmdList.get(cmdIndex)).length() >= cols)
            {
              while (yPos > 0)
              {
                System.out.print(escMoveUp);
                yPos--;
              }
              System.out.flush();
            }
            else
            {
              yPos = 0;
            }
          }
          else if (key == 2) // CTRL-B (do nothing)
          {
          }
          else if (key == 3) // CTRL-C (abort; quit without running any command)
          {
            for (int i=yPos; i<(((String)cmdList.get(cmdIndex)).length()-1)/cols; i++)
            {
              System.out.println("\r");
            }
            System.out.println("\r");
            System.out.println("\r");
          }
          else if (key == 5) // CTRL-E (move cursor to end of command string)
          {
            if (((String)cmdList.get(cmdIndex)).length() < cols)
            {
              System.out.print("\r");
              for (int i=0; i<((String) cmdList.get(cmdIndex)).length(); i++)
              {
                System.out.print(escMoveRight);
              }
              System.out.flush();
              xPos = ((String) cmdList.get(cmdIndex)).length();
              yPos = 0;
            }
            else
            {
              System.out.print("\r");
              for (int i=0; i<((String) cmdList.get(cmdIndex)).length() % cols; i++)
              {
                System.out.print(escMoveRight);
              }
              xPos = ((String) cmdList.get(cmdIndex)).length() % cols;
              while (yPos < ((String) cmdList.get(cmdIndex)).length() / cols)
              {
                System.out.print(escMoveDown);
                yPos++;
              }
              System.out.flush();
            }
          }
          else if ((key >= 6) && (key <= 12)) // CTRL-F thru CTRL-L (do nothing)
          {
          }
          else if (key == 13) // CTRL-M (or the Enter key)
          {
            for (int i=yPos; i<(((String)cmdList.get(cmdIndex)).length()-1)/cols; i++)
            {
              System.out.println("\r");
            }
            System.out.println("\r");
            System.out.println("\r");
            doneEditing = true;
          }
          else if ((key >= 14) && (key <= 19)) // CTRL-N thru CTRL-S (do nothing)
          {
          }
          else if (key == 20) // Ctrl-T (switch between insertMode and overtypeMode)
          {
            // TODO: Implement this feature
          }
          else if ((key >= 21) && (key <= 26)) // CTRL-U thru CTRL-Z (do nothing)
          {
          }
          else
          {
            if (insertMode)
            {
              if (key == 127)  // Backspace (delete character left of cursor)
              {
                if (((String)cmdList.get(cmdIndex)).length() < cols)
                {
                  if (xPos > 0)
                  {
                    String leftStr  = "";
                    String rightStr = "";
                    try { leftStr  = ((String)cmdList.get(cmdIndex)).substring(0, xPos - 1); } catch (Exception e) {}
                    try { rightStr = ((String)cmdList.get(cmdIndex)).substring(xPos, ((String)cmdList.get(cmdIndex)).length()); } catch (Exception e) {}
                    String newStr = leftStr + rightStr;
                    cmdList.set(cmdIndex, newStr);
                    System.out.print(escClearLine + "\r" + newStr + "\r");
                    xPos--;
                    for (int i=0; i<xPos; i++)
                    {
                      System.out.print(escMoveRight);
                    }
                    System.out.flush();
                  }
                }
                else
                {
                  int oldPos = yPos * cols + xPos;
                  if (oldPos > 0)
                  {
                    String leftStr  = "";
                    String rightStr = "";
                    try { leftStr  = ((String)cmdList.get(cmdIndex)).substring(0, oldPos - 1); } catch (Exception e) {}
                    try { rightStr = ((String)cmdList.get(cmdIndex)).substring(oldPos, ((String)cmdList.get(cmdIndex)).length()); } catch (Exception e) {}
                    String newStr = leftStr + rightStr;
                    cmdList.set(cmdIndex, newStr);
                    for (int i=0; i<yPos; i++)
                    {
                      System.out.print(escMoveUp);
                    }
                    System.out.print(escClearLine + "\r" + newStr + escClearToEol + escClearDown + "\r");
                    for (int i=0; i<(newStr.length()-1)/cols; i++)
                    {
                      System.out.print(escMoveUp);
                    }
                    int newPos = oldPos - 1;
                    yPos = newPos / cols;
                    xPos = newPos % cols;
                    for (int i=0; i<yPos; i++)
                    {
                      System.out.print(escMoveDown);
                    }
                    for (int i=0; i<xPos; i++)
                    {
                      System.out.print(escMoveRight);
                    }
                    System.out.flush();
                  }
                }
              }
              else if (key == 4)  // Ctrl-D (delete character under cursor)
              {
                if (((String)cmdList.get(cmdIndex)).length() < cols)
                {
                  if (xPos < ((String)cmdList.get(cmdIndex)).length())
                  {
                    String leftStr  = "";
                    String rightStr = "";
                    try { leftStr  = ((String)cmdList.get(cmdIndex)).substring(0, xPos); } catch (Exception e) {}
                    try { rightStr = ((String)cmdList.get(cmdIndex)).substring(xPos + 1, ((String)cmdList.get(cmdIndex)).length()); } catch (Exception e) {}
                    String newStr = leftStr + rightStr;
                    cmdList.set(cmdIndex, newStr);
                    System.out.print(escClearLine + "\r" + newStr + "\r");
                    for (int i=0; i<xPos; i++)
                    {
                      System.out.print(escMoveRight);
                    }
                    System.out.flush();
                  }
                }
                else
                {
                  int dPos = yPos * cols + xPos;
                  if (dPos < ((String)cmdList.get(cmdIndex)).length())
                  {
                    String leftStr  = "";
                    String rightStr = "";
                    try { leftStr  = ((String)cmdList.get(cmdIndex)).substring(0, dPos); } catch (Exception e) {}
                    try { rightStr = ((String)cmdList.get(cmdIndex)).substring(dPos + 1, ((String)cmdList.get(cmdIndex)).length()); } catch (Exception e) {}
                    String newStr = leftStr + rightStr;
                    cmdList.set(cmdIndex, newStr);
                    for (int i=0; i<yPos; i++)
                    {
                      System.out.print(escMoveUp);
                    }
                    System.out.print(escClearLine + "\r" + newStr + escClearToEol + escClearDown + "\r");
                    for (int i=0; i<(newStr.length()-1)/cols; i++)
                    {
                      System.out.print(escMoveUp);
                    }
                    for (int i=0; i<yPos; i++)
                    {
                      System.out.print(escMoveDown);
                    }
                    for (int i=0; i<xPos; i++)
                    {
                      System.out.print(escMoveRight);
                    }
                    System.out.flush();
                  }
                }
              }
              else
              {
                if (((String)cmdList.get(cmdIndex)).length() < cols)
                {
                  String leftStr  = "";
                  String rightStr = "";
                  try { leftStr  = ((String)cmdList.get(cmdIndex)).substring(0, xPos); } catch (Exception e) {}
                  try { rightStr = ((String)cmdList.get(cmdIndex)).substring(xPos, ((String)cmdList.get(cmdIndex)).length()); } catch (Exception e) {}
                  String newStr = leftStr + Character.toString((char) key) + rightStr;
                  cmdList.set(cmdIndex, newStr);
                  System.out.print(escClearLine + "\r" + newStr + "\r");
                  xPos++;
                  for (int i=0; i<xPos; i++)
                  {
                    System.out.print(escMoveRight);
                  }
                  System.out.flush();
                }
                else
                {
                  int oldPos = yPos * cols + xPos;
                  String leftStr  = "";
                  String rightStr = "";
                  try { leftStr  = ((String)cmdList.get(cmdIndex)).substring(0, oldPos); } catch (Exception e) {}
                  try { rightStr = ((String)cmdList.get(cmdIndex)).substring(oldPos, ((String)cmdList.get(cmdIndex)).length()); } catch (Exception e) {}
                  String newStr = leftStr + Character.toString((char) key) + rightStr;
                  cmdList.set(cmdIndex, newStr);
                  for (int i=0; i<yPos; i++)
                  {
                    System.out.print(escMoveUp);
                  }
                  System.out.print(escClearLine + "\r" + newStr + escClearToEol + escClearDown + "\r");
                  for (int i=0; i<(newStr.length()-1)/cols; i++)
                  {
                    System.out.print(escMoveUp);
                  }
                  int newPos = oldPos + 1;
                  yPos = newPos / cols;
                  xPos = newPos % cols;
                  for (int i=0; i<yPos; i++)
                  {
                    System.out.print(escMoveDown);
                  }
                  for (int i=0; i<xPos; i++)
                  {
                    System.out.print(escMoveRight);
                  }
                  System.out.flush();
                }
              }
            }
            else // if not in insert mode (not yet implemented)
            {
              System.out.println("key = " + key + "\r");
            }
          }
        }
      }

      if (doneEditing && (key != 3))
      {
        System.out.println("Executing command: " + cmdList.get(cmdIndex) + "\r");

        FileWriter fw = new FileWriter(configProps.getString("command_history_file"), true);
        fw.write(cmdList.get(cmdIndex) + "\n");
        fw.close();

        fw = new FileWriter(configProps.getString("output_file"));

        RemoteRunnerThread.threadCount = hostnameList.size();
        for (int i=0; i<hostnameList.size(); i++)
        {
          String hostname = (String) hostnameList.get(i);
          RemoteRunnerThread rrt = new RemoteRunnerThread();
          rrt.setName(hostname);
          rrt.setHostname(hostname);
          rrt.setCommand(cmdList.get(cmdIndex));
          rrt.setOutputFileWriter(fw);
          rrt.start();
        }

        while (RemoteRunnerThread.threadCount > 0)
        {
          Thread.sleep(10);
        }

        fw.close();
      }
      else // if CTRL-C was pressed...
      {
        System.out.println("Quitting without executing any commands.\r");
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
