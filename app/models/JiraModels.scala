package models

import java.net.URI
import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.BaseFormats._

case class JiraAvatarUrls(l16x16: String, l24x24: String, l32x32: String, l48x48: String)
case class JiraProject(self: URI, id: String, key: String, name: String, avatarUrls: JiraAvatarUrls)
case class JiraIssueType(self: URI, id: String, description: String, iconUrl: String, name: String, subtask: Boolean)
case class JiraVotes(self: URI, votes: Number, hasVoted: Boolean)
case class JiraFixVersion(self: URI, id: String, description: String, name: String, archived: Boolean, released: Boolean)
case class JiraPerson(self: URI, name: String, emailAddress: String, avatarUrls: JiraAvatarUrls, displayName: String, active: Boolean)
case class JiraProgress(progress: Number, total: Number)
case class JiraPriority(self: URI, iconUrl: String, name: String, id: String)
case class JiraLinkType(id: String, name: String, inward: String, outward: String, self: URI)
case class JiraWatches(self: URI, watchCount: Number, isWatching: Boolean)
case class JiraIssueLink(id: String, self: URI, `type`: JiraLinkType, inwardIssue: JiraIssue)
case class JiraVersion(self: URI, id: String, description: String, name: String, archived: Boolean, released: Boolean)
case class JiraStatusCategory(self: URI, id: Number, key: String, colorName: String, name: String)
case class JiraStatus(self: URI, description: String, iconUrl: String, name: String, id: String, statusCategory: JiraStatusCategory)
case class JiraLabel(name: String)
case class JiraComponent(self: URI, id: Long, name: String, lead: JiraPerson)
case class PrimaryJiraIssueFields(summary: String,
  progress: Option[JiraProgress],
  issuetype: JiraIssueType,
  votes: Option[JiraVotes],
  resolution: String,
  resolutiondate: String,
  timespent: String,
  creator: Option[JiraPerson],
  reporter: Option[JiraPerson],
  aggregatetimeoriginalestimate: String,
  created: Date,
  updated: Date,
  description: String,
  priority: JiraPriority,
  duedate: Date,
  watches: JiraWatches,
  status: JiraStatus,
  workratio: Number,
  project: JiraProject,
  aggregateprogress: JiraProgress,
  lastViewed: Date)

case class SecondaryJiraIssueFields(issuelinks: Seq[JiraIssueLink],
  subtasks: Seq[JiraIssue],
  labels: Seq[JiraLabel],
  assignee: Option[JiraPerson],
  aggregatetimeestimate: Option[Number],
  versions: Seq[JiraVersion],
  fixVersions: Seq[JiraVersion],
  environment: Option[String],
  timeestimate: Option[Date],
  components: Seq[JiraComponent],
  timeoriginalestimate: Option[String],
  aggregatetimespent: Option[Number])

case class JiraIssueFields(
  primary: PrimaryJiraIssueFields,
  secondary: SecondaryJiraIssueFields)

case class JiraIssue(expand: String, id: Number, self: URI, key: String, fields: JiraIssueFields)

object BaseFormats {
  implicit object URIFormat extends Format[URI] {
    def writes(uri: URI): JsValue = {
      JsString(uri.toURL().toExternalForm())
    }
    def reads(json: JsValue): JsResult[URI] = json match {
      case JsString(x) => {
        JsSuccess(new URI(x))
      }
      case _ => JsError("Expected URI as JsString")
    }
  }

  implicit object NumberFormat extends Format[Number] {
    def writes(nb: Number): JsValue = {
      JsNumber(BigDecimal(nb.doubleValue()))
    }
    def reads(json: JsValue): JsResult[Number] = json match {
      case JsNumber(x) => {
        JsSuccess(x.toDouble)
      }
      case _ => JsError("Expected URI as JsString")
    }
  }
}

object JiraAvatarUrls {
  implicit val jsonFormat: Format[JiraAvatarUrls] = Json.format[JiraAvatarUrls]
}
object JiraProject {
  implicit val jsonFormat: Format[JiraProject] = Json.format[JiraProject]
}
object JiraIssueType {
  implicit val jsonFormat: Format[JiraIssueType] = Json.format[JiraIssueType]
}
object JiraVotes {
  implicit val jsonFormat: Format[JiraVotes] = Json.format[JiraVotes]
}
object JiraFixVersion {
  implicit val jsonFormat: Format[JiraFixVersion] = Json.format[JiraFixVersion]
}
object JiraPerson {
  implicit val jsonFormat: Format[JiraPerson] = Json.format[JiraPerson]
}
object JiraProgress {
  implicit val jsonFormat: Format[JiraProgress] = Json.format[JiraProgress]
}
object JiraPriority {
  implicit val jsonFormat: Format[JiraPriority] = Json.format[JiraPriority]
}
object JiraLinkType {
  implicit val jsonFormat: Format[JiraLinkType] = Json.format[JiraLinkType]
}
object JiraWatches {
  implicit val jsonFormat: Format[JiraWatches] = Json.format[JiraWatches]
}
object JiraIssueLink {
  implicit val jsonFormat: Format[JiraIssueLink] = Json.format[JiraIssueLink]
}
object JiraVersion {
  implicit val jsonFormat: Format[JiraVersion] = Json.format[JiraVersion]
}
object JiraStatusCategory {
  implicit val jsonFormat: Format[JiraStatusCategory] = Json.format[JiraStatusCategory]
}
object JiraStatus {
  implicit val jsonFormat: Format[JiraStatus] = Json.format[JiraStatus]
}
object JiraLabel {
  implicit val jsonFormat: Format[JiraLabel] = Json.format[JiraLabel]
}
object JiraComponent {
  implicit val jsonFormat: Format[JiraComponent] = Json.format[JiraComponent]
}
object JiraIssue {
  implicit val jsonFormat: Format[JiraIssue] = Json.format[JiraIssue]
}
object PrimaryJiraIssueFields {
  implicit val jsonFormat: Format[PrimaryJiraIssueFields] = Json.format[PrimaryJiraIssueFields]
}
object SecondaryJiraIssueFields {
  implicit val jsonFormat: Format[SecondaryJiraIssueFields] = Json.format[SecondaryJiraIssueFields]
}

object JiraIssueFields {
  implicit val issueFieldsReads: Reads[JiraIssueFields] = (
    (JsPath).read[PrimaryJiraIssueFields] and
    (JsPath).read[SecondaryJiraIssueFields])(JiraIssueFields.apply _)

  implicit val issueFieldsWrites: Writes[JiraIssueFields] = (
    (JsPath).write[PrimaryJiraIssueFields] and
    (JsPath).write[SecondaryJiraIssueFields])(unlift(JiraIssueFields.unapply))
}

