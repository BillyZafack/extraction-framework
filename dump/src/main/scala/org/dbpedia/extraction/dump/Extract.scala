package org.dbpedia.extraction.dump

import java.util.logging.{Logger, FileHandler}
import java.io.File

/**
 * Dump extraction script.
 */
object Extract
{
    def main(args : Array[String]) : Unit =
    {
        require(args != null && args.length == 1 && args(0).nonEmpty, "missing argument: config file name")
        
        val logHandler = new FileHandler("./log.xml")
        Logger.getLogger("org.dbpedia.extraction").addHandler(logHandler)

        val extraction = new ExtractionThread(args(0))
        extraction.start()
        extraction.join()
    }

    private class ExtractionThread(fileName : String) extends Thread
    {
        override def run
        {
            val logger = Logger.getLogger("org.dbpedia.extraction");

            val configFile = new File(fileName);
            logger.info("Loading config from '" + configFile.getCanonicalPath + "'");

            //Load extraction jobs from configuration
            val extractionJobs = ConfigLoader.load(configFile)

            //Execute the extraction jobs one by one and print the progress to the console
            for(extractionJob <- extractionJobs)
            {
                // TODO: why this check? who should interrupt this thread?
                if(isInterrupted) return

                extractionJob.start()

                try
                {
                    // FIXME: why use a thread when we just wait here for it to finish?
                  
                    while(extractionJob.isAlive)
                    {
                        val progress = extractionJob.progress
                        if(progress.startTime.get > 0)
                        {
                            val time = (System.currentTimeMillis - progress.startTime.get).toDouble
                            println("Extracted " + progress.extractedPages.get + " pages (Per page: " + (time / progress.extractedPages.get) + " ms; Failed pages: " + progress.failedPages.get + ").")
                        }

                        Thread.sleep(2000L)
                    }
                    println
                }
                catch
                {
                    case _ : InterruptedException =>
                    {
                        println("Shutting down...")
                        extractionJob.interrupt()
                        extractionJob.join()
                        return
                    }
                }

            }
        }
    }
}
