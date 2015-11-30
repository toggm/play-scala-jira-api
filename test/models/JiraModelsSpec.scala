package models

import org.specs2.mutable._

import org.specs2.mutable.Specification
import org.specs2.runner._
import scala.io.Source
import play.api.libs.json.Json

class JiraModelsSpec extends Specification {
  "JSON Result" should {
    "parse jira issue correctly" in {
      val s = Source.fromFile("test/resources/jira_issue.json")
      val result = s.mkString

      val json = Json.parse(result)
      val opt = Json.fromJson[JiraIssue](json).asOpt
      opt must not be None

      val issue = opt.get
      issue.id === "11720"
    }

    "parse jira project correctly" in {
      val s = Source.fromFile("test/resources/jira_project.json")
      val result = s.mkString

      val json = Json.parse(result)
      val opt = Json.fromJson[JiraProject](json).asOpt
      opt must not be None

      val project = opt.get
      project.id === "10070"
    }

    "parse jira projects correctly" in {
      val s = Source.fromFile("test/resources/jira_projects.json")
      val result = s.mkString

      val json = Json.parse(result)
      val opt = Json.fromJson[Seq[JiraProject]](json).asOpt
      opt must not be None

      val projects = opt.get
      projects.size === 17
      projects(0).id === "10070"
    }

    "parse search result correctly" in {
      val s = Source.fromFile("test/resources/jira_search_result.json")
      val result = s.mkString

      val json = Json.parse(result)
      val opt = Json.fromJson[JiraSearchResult](json).asOpt
      opt must not be None
    }
    
    "parse search result2 correctly" in {
      val s = Source.fromFile("test/resources/jira_search_result2.json")
      val result = s.mkString

      val json = Json.parse(result)
      val opt = Json.fromJson[JiraSearchResult](json).asOpt
      opt must not be None
    }

    "parse search results correctly" in {
      val s = Source.fromFile("test/resources/jira_search_results.json")
      val result = s.mkString

      val json = Json.parse(result)
      val r = Json.fromJson[JiraSearchResult](json)
      val opt = r.asOpt
      opt must not be None

      val res = opt.get
      res.issues.size === 37
    }

    "parse simple jira issue correctly" in {
      val s = Source.fromFile("test/resources/jira_issue_simple.json")
      val result = s.mkString

      val json = Json.parse(result)
      val opt = Json.fromJson[JiraIssue](json).asOpt
      opt must not be None

      val issue = opt.get
      issue.id === "11720"
    }

    "parse jira version correctly" in {
      val json = Json.obj("self" -> "http://test.com", "id" -> "1", "description" -> "version1", "name" -> "1.0", "archived" -> false, "released" -> true)
      val opt = Json.fromJson[JiraVersion](json).asOpt
      opt must not be None

      val issue = opt.get
      issue.id === "1"
    }
  }

}