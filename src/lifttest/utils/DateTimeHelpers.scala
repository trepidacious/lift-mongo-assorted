package lifttest.utils
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import java.util.Locale
import net.liftweb.common._
import org.joda.time.DateTimeZone
import net.liftweb.util._
import Helpers._

object DateTimeHelpers extends DateTimeHelpers

trait DateTimeHelpers {

  private  lazy val logger = Logger(classOf[DateTimeHelpers])

  val dateTimeWithTimeFormat = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm")

  val dateTimeFormat = DateTimeFormat.forPattern("yyyy/MM/dd")
    
  val internetDateTimeFormat = DateTimeFormat.forPattern("EEE, d MMM yyyy HH:mm:ss 'GMT'").withLocale(Locale.US).withZone(DateTimeZone.UTC)

  def parseDateTime(dateString: String): Box[DateTime] = tryo {
    dateTimeFormat.parseDateTime(dateString)
  }  

  def printDateTime(in: DateTime): String = dateTimeFormat.print(in)

  def parseInternetDateTime(dateString: String): DateTime = tryo {
    internetDateTimeFormat.parseDateTime(dateString)
  } openOr new DateTime(0L)
  
  /** @return a date from a string using the internet format. Return the Epoch date if the parse is unsuccessful */
  def boxParseInternetDateTime(dateString: String): Box[DateTime] = tryo {
    internetDateTimeFormat.parseDateTime(dateString)
  }

  def toInternetDateTime(in: DateTime): String = internetDateTimeFormat.print(in)
  
    /** @return a Full(DateTime) or a failure if the input couldn't be translated to DateTime (or Empty if the input is null)*/
  def toDateTime(in: Any): Box[DateTime] = {
    try {
      in match {
        case null => Empty
        case d: DateTime => Full(d)
        case d: java.util.Date => Full(new DateTime(d))
        case c: java.util.Calendar => Full(new DateTime(c))
        //Note that JDK and Joda DateTime epoch are the same, we assume that we are dealing with milliseconds since epoch
        case lng: Long => Full(new DateTime(lng))
        case lng: Number => Full(new DateTime(lng.longValue))
        case Nil | Empty | None | Failure(_, _, _) => Empty
        case Full(v) => toDateTime(v)
        case Some(v) => toDateTime(v)
        case v :: vs => toDateTime(v)
        case s : String => tryo(internetDateTimeFormat.parseDateTime(s)) or tryo(dateTimeFormat.parseDateTime(s))
        case o => toDateTime(o.toString)
      }
    } catch {
      case e => logger.debug("Error parsing Joda DateTime "+in, e); Failure("Bad Joda DateTime: "+in, Full(e), Empty)
    }
    
  }

}
