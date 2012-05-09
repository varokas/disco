package com.huskycode.disco.executor

class Executor {

  def execute() = {
    val commands = parseConfigToCommands()
	  commands.foreach( c => c.execute() )	

	}
  private def parseConfigToCommands():Iterable[Command] = {
		return null
	}
}

trait Command {
  def execute():CommandResult

}

trait CommandResult {
}

class ReaderCommand extends Command {
	override def execute():CommandResult = { return null }
}

class RunnerCommand extends Command {

	override def execute():CommandResult = {return null }
}

