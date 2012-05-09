package com.huskycode.disco.runner

class CommandRunner(cmd: String) {
  import java.io._
  
  val rt = Runtime.getRuntime()
  
  def runCommand():CommandResult = {
    val pr = rt.exec(cmd)
    val input = new BufferedReader(new InputStreamReader(pr.getInputStream()))
    var line:String = null
    val sb = new StringBuilder()
    while({ line = input.readLine(); line != null }) {
      sb.append(line + "\n")
    }
    val exitVal = pr.waitFor()
    new CommandResult(exitVal, sb.toString)	  
  }
}

case class CommandResult(exitVal:Int, output:String) {
}
