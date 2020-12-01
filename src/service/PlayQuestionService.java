package service;

import dataObjects.Answer;
import dataObjects.Question;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import service.progress.PlayQuestionProgress;

import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class PlayQuestionService {
    private static final PlayQuestionService instance = new PlayQuestionService();

    public static PlayQuestionService getInstance() {
        return instance;
    }

    private final List<PlayQuestionProgress> currentlyRunningQuestions = new ArrayList<>();
    private final Random random= new Random();

    private PlayQuestionService(){

    }

    /**
     * A user answered a question. Let's see what question we is referring to and tell him if he was right.
     * Also delete the question if he should be right. In case he is right everybody knows the answer.
     * @param contentRaw the message sent containing the answer
     * @param channel the channel the answer was posted in
     * @param author the author of the answer
     */
    public void answerCommandCalled(String contentRaw, MessageChannel channel, User author) {
        Integer indexOfAnswerChosen = getIndexOfAnswerChosen(contentRaw, channel);
        if(indexOfAnswerChosen == null){
            return;
        }
        PlayQuestionProgress questionProgressInChannel = getQuestionProgressInChannel(channel);
        if(questionProgressInChannel == null){
            channel.sendMessage("No question found that could be answered").queue();
            return;
        }
        Boolean correct = isAnswerCorrect(indexOfAnswerChosen, questionProgressInChannel, channel);
        if(correct == null){
            return;
        }
        if(correct){
            praiseUserForCorrectAnswer(author, channel);
        }else {
            informChosenAnswerIsWrong(author, channel);
        }

    }

    private void informChosenAnswerIsWrong(User author, MessageChannel channel) {
        channel.sendMessage("""
                %s, we are sorry your answer was wrong.
                """.formatted(author.getName())).queue();
    }

    private void praiseUserForCorrectAnswer(User author, MessageChannel channel) {
        channel.sendMessage("""
                %s, congratulations your answer was right.
                """.formatted(author.getName())).queue();
    }

    private Boolean isAnswerCorrect(Integer indexOfAnswerChosen, PlayQuestionProgress questionProgressInChannel, MessageChannel channel) {
        List<Answer> answers = questionProgressInChannel.getQuestion().getAnswers();
        if(indexOfAnswerChosen<0 || answers.size()<= indexOfAnswerChosen){
            channel.sendMessage("There is no answer with index %d".formatted(indexOfAnswerChosen+1)).queue();
            return null;
        }
        return answers.get(indexOfAnswerChosen).isCorrect();
    }

    private Integer getIndexOfAnswerChosen(String contentRaw, MessageChannel channel) {
        Pattern patterOfAnswerCommand = Pattern.compile("!answer (\\d+)");
        Matcher matcher = patterOfAnswerCommand.matcher(contentRaw);
        if(!matcher.matches()){
            channel.sendMessage("Invalid command").queue();
            return null;
        }
        return Integer.parseInt(matcher.group(1))-1;
    }

    /**
     * A user wants to play a question. Let's print the question and wait for answers.
     * @param channel the channel the message was sent in
     */
    public void questionCommandCalled(MessageChannel channel) {
        Question randomQuestion = getRandomQuestion(channel);
        if(randomQuestion==null){
            return;
        }
        askQuestion(channel, randomQuestion);
        currentlyRunningQuestions.add(new PlayQuestionProgress(randomQuestion, channel));
    }

    private void askQuestion(MessageChannel channel, Question randomQuestion) {
        String message= """
                You have asked for a question. Here is your question: %s
                You have the following possibilities to answer:
                """.formatted(randomQuestion.getQuestion());
        List<Answer> answers = randomQuestion.getAnswers();
        for (int i = 0; i < answers.size(); i++) {
            Answer answer = answers.get(i);
            message += """
                    Write !answer %d to answer: %s
                    """.formatted(i+1, answer.getAnswer());
        }
        channel.sendMessage(message).queue();
    }

    /**
     *
     * @return a random Question using the QuestionStorageService
     */
    private Question getRandomQuestion(MessageChannel channel) {
        List<Question> questions = QuestionStorageService.getInstance()
                .getQuestions();
        if(questions.isEmpty()){
            channel.sendMessage("No messages available").queue();
            return null;
        }
        return questions
                .get(random.nextInt(questions.size()));
    }

    /**
     * Somebody wants to abort. Let's see if we have a question running in the channel. If so we want to delete it.
     * @param message the message that contains an !abort
     */
    public void abort(Message message) {
        PlayQuestionProgress questionProgressInChannel = getQuestionProgressInChannel(message.getChannel());
        currentlyRunningQuestions.remove(questionProgressInChannel);
    }

    private PlayQuestionProgress getQuestionProgressInChannel(MessageChannel channel) {
        for (PlayQuestionProgress playQuestionProgress:
             currentlyRunningQuestions) {
            if(playQuestionProgress.getChannel().equals(channel))
                return playQuestionProgress;
        }
        return null;
    }


}
