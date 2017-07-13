package com.ft.jenkins.slack

import com.ft.jenkins.Cluster
import com.ft.jenkins.Environment
import com.ft.jenkins.EnvsRegistry

import static com.ft.jenkins.slack.SlackConstants.BOT_USERNAME
import static com.ft.jenkins.slack.SlackConstants.DEFAULT_CREDENTIALS

/**
 * Utils for Slack
 */

final class SlackConstants {
  public static final String DEFAULT_CREDENTIALS = "ft.slack.bot-credentials"
  public static final String BOT_USERNAME = "jenkins-bot"
}

public void sendEnvSlackNotification(String environment, String message) {
  sendSlackNotification(EnvsRegistry.getSlackChannelForEnv(environment), message)
}

public String getHealthUrl(Environment environment, Cluster cluster, String region = null) {
  String entryPointURL = environment.getEntryPointUrl(cluster, region)
  String fullClusterName = environment.getFullClusterName(cluster, region)
  return "<${entryPointURL}/__health|${fullClusterName}>"
}


/**
 * Sends a slack notification using the https://api.slack.com/methods/chat.postMessage method.
 *
 * @param channel the channel where to send the notification. Example: @username, #upp-tech
 * @param message The message to send
 * @param credentialId the id of Jenkins credentials to use. By default it will use 'ft.slack.bot-credentials'
 */
public void sendSlackNotification(String channel, String message, String credentialId = DEFAULT_CREDENTIALS) {
  String encodedChannel = URLEncoder.encode(channel, "UTF-8")
  String encodedMessage = URLEncoder.encode(message, "UTF-8")
  withCredentials([[$class: 'StringBinding', credentialsId: credentialId, variable: 'SLACK_TOKEN']]) {
    sh """
    curl -X POST -s \\
  https://slack.com/api/chat.postMessage \\
  -H 'cache-control: no-cache' \\
  -H 'content-type: application/x-www-form-urlencoded' \\
  -d 'text=${encodedMessage}&token=${env.SLACK_TOKEN}&channel=${encodedChannel}&username=${BOT_USERNAME}'
  """
  }
}


public void sendEnhancedSlackNotification(String channel, SlackAttachment attachment,
                                          String credentialId = DEFAULT_CREDENTIALS) {

  /*  not using the JsonBuilder as we need NonCPS for that, and seems it doesn't play well with looking up credentials */
  String attachmentJson="""[{
    "pretext": "${attachment.preText}",
    "author_name": "${attachment.authorName}",
    "author_link": "${attachment.authorLink}",
    "author_icon": "${attachment.authorIcon}",
    "title": "${attachment.title}",
    "title_link": "${attachment.titleUrl}",
    "text": "${attachment.text}",
    "image_url": "${attachment.imageUrl}",
    "thumb_url": "${attachment.thumbUrl}",
    "footer": "${attachment.footer}",
    "footer_icon": "${attachment.footerIcon}",
    "color": "${attachment.color}",
    "mrkdwn_in": ["text", "pretext"]
    ${attachment.timestamp ? ', "ts": ' + attachment.timestamp : "" }
  }]"""

  String encodedChannel = URLEncoder.encode(channel, "UTF-8")
  String encodedAttachment = URLEncoder.encode(attachmentJson, "UTF-8")
  echo "Sending attachment to slack: ${attachmentJson}"
  withCredentials([[$class: 'StringBinding', credentialsId: credentialId, variable: 'SLACK_TOKEN']]) {
    String requestBody = "token=${env.SLACK_TOKEN}&attachments=${encodedAttachment}&channel=${encodedChannel}&username=${BOT_USERNAME}"
    echo "Whole request body: ${requestBody}"
    httpRequest(httpMode: 'POST',
                url: 'https://slack.com/api/chat.postMessage',
                customHeaders: [[maskValue: false, name: 'content-type', value: 'application/x-www-form-urlencoded']],
                timeout: 10,
                consoleLogResponseBody: true,
                requestBody: requestBody)
  }
}
