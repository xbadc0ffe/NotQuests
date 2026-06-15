package rocks.gravili.notquests.paper.managers.integrations.betonquest.conversationInterceptors;

import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.conversation.interceptor.Interceptor;
import org.betonquest.betonquest.conversation.interceptor.InterceptorFactory;
import rocks.gravili.notquests.paper.NotQuests;

public class NotQuestsInterceptorFactory implements InterceptorFactory {
  private final NotQuests main;

  public NotQuestsInterceptorFactory(final NotQuests main) {
    this.main = main;
  }

  @Override
  public Interceptor create(final OnlineProfile onlineProfile) {
    return new NotQuestsInterceptor(main, onlineProfile);
  }
}
