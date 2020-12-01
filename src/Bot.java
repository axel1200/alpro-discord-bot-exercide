import dataObjects.Question;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.PlayQuestionService;
import service.QuestionCreationService;
import service.progress.PlayQuestionProgress;

import javax.security.auth.login.LoginException;

public class Bot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) throws LoginException {
        JDABuilder jdaBuilder = JDABuilder.createDefault("NzgzMzYzMjU3MDg3MTY0NDQ2.X8Zp4g.kZQExzQpeAlXIEbHNOTPadpuGM0");

        JDA build = jdaBuilder.build();
        build.addEventListener(new Bot());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try{
            logger.info("Received message with text: {}", event.getMessage().getContentRaw());
            handleMessage(event);
        }catch (Exception e){
            logger.warn("Could not process message", e);
            event.getMessage().getChannel().sendMessage("""
                    Unexpected error occurred!
                    """).queue();

        }
    }

    private void handleMessage(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        Message message = event.getMessage();
        String content = message.getContentRaw();
        if(content.equals("!abort")){
            QuestionCreationService
                    .getInstance()
                    .abort(message);
            PlayQuestionService
                    .getInstance()
                    .abort(message);
        } else if (content.equals("!createQuestion")) {
            QuestionCreationService
                    .getInstance()
                    .createQuestion(message);
        } else if(content.equals("!question")){
            PlayQuestionService
                    .getInstance()
                    .questionCommandCalled(message.getChannel());
        }else if(content.contains("!answer")){
            PlayQuestionService
                    .getInstance()
                    .answerCommandCalled(content, message.getChannel(), message.getAuthor());
        }else if(QuestionCreationService.getInstance().isQuestionCreationRunning(message.getChannel(), message.getAuthor())){
            QuestionCreationService
                    .getInstance()
                    .infoAboutQuestionInCreation(message);
        }
    }
}
