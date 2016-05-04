name := "SumCrawlerConsumer"

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "SpinGo OSS" at "http://spingo-oss.s3.amazonaws.com/repositories/releases",
  "RoundEights" at "http://maven.spikemark.net/roundeights"
)

val opRabbitVersion = "1.3.0"

libraryDependencies ++= Seq(
  "org.scalikejdbc"  %% "scalikejdbc"           % "2.3.5",
  "org.postgresql"   %  "postgresql"            % "9.3-1100-jdbc41",
  "ch.qos.logback"   %  "logback-classic"       % "1.1.3",
  "com.roundeights"  %% "hasher"                % "1.2.0",
  "com.spingo"       %% "op-rabbit-core"        % opRabbitVersion,
  "com.spingo"       %% "op-rabbit-play-json"   % opRabbitVersion,
  "com.spingo"       %% "op-rabbit-json4s"      % opRabbitVersion,
  "com.spingo"       %% "op-rabbit-airbrake"    % opRabbitVersion,
  "com.spingo"       %% "op-rabbit-akka-stream" % opRabbitVersion,
  "com.jsuereth"     %  "gpg-library_2.10"      % "0.8.3",
  "org.specs2"       %% "specs2-core"           % "3.8" % "test"
)

scalacOptions in Test ++= Seq("-Yrangepos")

flywayDriver := "org.postgresql.Driver"

flywayUrl := "jdbc:postgresql://localhost:5432/sumcrawler"

flywayUser := "postgres"

flywayPassword := "passw0rd"

