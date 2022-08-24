def sout = new StringBuilder()
  def serr = new StringBuilder()
  def url = ""
  def cmd = ""
  def proc = cmd.execute()
  proc.consumeProcessOutput(sout, serr)
  proc.waitForOrKill(60000)
  log.warn(sout)
  if(serr){
      log.error(("Error executing command: $cmd \n$serr"))
      Response.status(400).build()
  } 
  else {
      return Response.ok(sout.replaceAll("[\n\r]", "")).build()
  }
