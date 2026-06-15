package rocks.gravili.notquests.paper.conversation;

public class Speaker {
  private final String speakerName;
  private final String speakerDisplayName;

  private String color = "<WHITE>";
  private int talkSpeed = 10;
  private boolean player = false;

  private int delayInMS;

  private final Conversation conversation;

  public Speaker(final String speakerName, final Conversation conversation) {
    this.speakerName = speakerName;
    this.speakerDisplayName =
        speakerName.replace("__", "{UNDERSCORE}").replace("_", " ").replace("{UNDERSCORE}", "_");

    this.conversation = conversation;
    this.delayInMS = conversation.getDelayInMS();
  }

  public final String getSpeakerName() {
    return speakerName;
  }

  public final String getSpeakerDisplayName() {
    return speakerDisplayName;
  }

  public final String getColor() {
    return color;
  }

  public void setColor(final String newColor) {
    this.color = newColor;
  }

  public final int getTalkSpeed() {
    return talkSpeed;
  }

  public void setTalkSpeed(final int talkSpeed) {
    this.talkSpeed = talkSpeed;
  }

  public final boolean isPlayer() {
    return player;
  }

  public void setPlayer(final boolean player) {
    this.player = player;
  }

  public void setDelayInMS(final int delayInMS) {
    this.delayInMS = delayInMS;
  }

  public final int getDelayInMS() {
    return delayInMS;
  }

  public final Conversation getConversation(){
    return conversation;
  }
}
