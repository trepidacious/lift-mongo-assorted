package lifttest.model
import net.liftweb.record._
import org.joda.time.DateTime
import net.liftweb.common._
import scala.xml._
import net.liftweb.common._
import net.liftweb.http.{S}
import net.liftweb.http.js._
import net.liftweb.json.JsonAST.JValue
import net.liftweb.util._
import Helpers._
import S._
import JE._
import org.joda.time.format.DateTimeFormat
import java.util.Locale
import org.joda.time.DateTimeZone
import lifttest.utils.DateTimeHelpers._

trait JodaDateTimeTypedField extends TypedField[DateTime] {
  
  def setFromAny(in : Any): Box[DateTime] = toDateTime(in).flatMap(d => setBox(Full(d))) or genericSetFromAny(in)

  def setFromString(s: String): Box[DateTime] = s match {
    case "" if optional_? => setBox(Empty)
    case other            => setBox(tryo(parseInternetDateTime(s)))
  }

  private def elem =
    S.fmapFunc(SFuncHolder(this.setFromAny(_))){funcName =>
      <input type="text"
        name={funcName}
        value={valueBox.map(s => toInternetDateTime(s)) openOr ""}
        tabindex={tabIndex toString}/>
    }

  def toForm: Box[NodeSeq] =
    uniqueFieldId match {
      case Full(id) => Full(elem % ("id" -> id))
      case _        => Full(elem)
    }

  def asJs = valueBox.map(v => Str(toInternetDateTime(v))) openOr JsNull

  def asJValue = asJString(v => toInternetDateTime(v))
  
  def setFromJValue(jvalue: JValue) = setFromJString(jvalue) {
    v => boxParseInternetDateTime(v)
  }
}

class JodaDateTimeField[OwnerType <: Record[OwnerType]](rec: OwnerType)
  extends Field[DateTime, OwnerType] with MandatoryTypedField[DateTime] with JodaDateTimeTypedField {

  def owner = rec

  def this(rec: OwnerType, value: DateTime) = {
    this(rec)
    setBox(Full(value))
  }

  def defaultValue = new DateTime()
  
  def asStandardString = valueBox.map(dt => dateTimeWithTimeFormat.print(dt))
}
