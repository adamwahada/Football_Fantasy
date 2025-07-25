package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Dto.GameweekPredictionSubmissionDTO;
import FootballFantasy.fantasy.Dto.GameweekPreviewDTO;
import FootballFantasy.fantasy.Dto.PredictionDTO;
import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Repositories.GameweekRepository.*;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private SessionParticipationRepository sessionParticipationRepository;

    @Autowired
    private SessionParticipationService sessionParticipationService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameWeekRepository gameweekRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionTemplateRepository sessionTemplateRepository ;

    // 🎯 STEP 1: Get all matches for a gameweek (preview before joining)
    public List<Match> getGameweekMatches(Long gameweekId) {
        GameWeek gameweek = gameweekRepository.findById(gameweekId)
                .orElseThrow(() -> new RuntimeException("Gameweek not found"));

        List<Match> matches = matchRepository.findByGameweeksId(gameweekId);

        if (matches.isEmpty()) {
            throw new RuntimeException("No matches found for this gameweek");
        }

        return matches;
    }

    // 🎲 Generate consistent tiebreaker matches for preview
    public List<Long> generateTiebreakerMatches(Long gameweekId) {
        List<Match> matches = getGameweekMatches(gameweekId);

        // Use gameweek ID as seed for consistent tiebreaker selection
        Random random = new Random(gameweekId);
        Collections.shuffle(matches, random);

        return matches.stream()
                .limit(Math.min(3, matches.size()))
                .map(Match::getId)
                .collect(Collectors.toList());
    }
    @Transactional
    public Map<String, Object> submitPredictionsAndJoinSession(GameweekPredictionSubmissionDTO submissionDTO,
                                                               SessionType sessionType,
                                                               BigDecimal buyInAmount,
                                                               boolean isPrivate,
                                                               String accessKey) {

        // ✅ STEP 1: Join or create session
        SessionParticipation participation = sessionParticipationService.joinCompetition(
                submissionDTO.getGameweekId(),
                submissionDTO.getCompetition(),
                sessionType,
                buyInAmount,
                isPrivate,
                accessKey,
                submissionDTO.getUserId()
        );

        // ✅ STEP 2: Save predictions with participation
        List<Prediction> savedPredictions = submitPredictions(submissionDTO, participation);

        return Map.of(
                "predictions", savedPredictions,
                "sessionParticipation", participation
        );
    }

    @Transactional
    public List<Prediction> submitPredictions(GameweekPredictionSubmissionDTO submissionDTO,
                                              SessionParticipation participation) {
        UserEntity user = userRepository.findById(submissionDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        GameWeek gameWeek = gameweekRepository.findById(submissionDTO.getGameweekId())
                .orElseThrow(() -> new RuntimeException("Gameweek not found"));

        List<Match> allMatches = matchRepository.findByGameweeksId(gameWeek.getId());
        if (allMatches.isEmpty()) {
            throw new RuntimeException("No matches found for this gameweek");
        }

        // 🧠 Get tiebreakers from GameWeek entity
        List<Long> tieBreakerIds = gameWeek.getTiebreakerMatchIdList();

        // 🛡️ Validate: ensure user submitted all predictions
        Set<Long> submittedMatchIds = submissionDTO.getPredictions().stream()
                .map(PredictionDTO::getMatchId).collect(Collectors.toSet());
        Set<Long> requiredMatchIds = allMatches.stream().map(Match::getId).collect(Collectors.toSet());

        if (!submittedMatchIds.equals(requiredMatchIds)) {
            throw new RuntimeException("Submitted predictions must cover all matches");
        }

        // Create predictions
        List<Prediction> predictions = submissionDTO.getPredictions().stream()
                .map(dto -> {
                    Match match = matchRepository.findById(dto.getMatchId())
                            .orElseThrow(() -> new RuntimeException("Match not found"));

                    boolean isTiebreaker = tieBreakerIds.contains(match.getId());

                    // ⛔ Winner-only allowed for normal matches
                    if (!isTiebreaker && (dto.getPredictedHomeScore() != null || dto.getPredictedAwayScore() != null)) {
                        throw new RuntimeException("Score prediction not allowed for non-tiebreaker matches");
                    }

                    // ⛔ Exact score required for tiebreaker matches
                    if (isTiebreaker && (dto.getPredictedHomeScore() == null || dto.getPredictedAwayScore() == null)) {
                        throw new RuntimeException("Exact score required for tiebreaker matches");
                    }

                    return Prediction.builder()
                            .user(user)
                            .match(match)
                            .predictedResult(dto.getPredictedResult())
                            .predictedHomeScore(isTiebreaker ? dto.getPredictedHomeScore() : null)
                            .predictedAwayScore(isTiebreaker ? dto.getPredictedAwayScore() : null)
                            .isTiebreaker(isTiebreaker)
                            .participation(participation)
                            .predictionTime(LocalDateTime.now())
                            .build();
                }).collect(Collectors.toList());

        // 🔥 THIS IS THE MISSING PIECE - SAVE TO DATABASE!
        return predictionRepository.saveAll(predictions);
    }



    // 📊 Calculate accuracy after matches are completed
    @Transactional
    public void calculatePredictionAccuracy(Long participationId) {
        System.out.println("🔍 [START] calculatePredictionAccuracy for participationId: " + participationId);

        SessionParticipation participation = sessionParticipationRepository.findById(participationId)
                .orElseThrow(() -> new RuntimeException("Participation not found"));

        List<Prediction> completedPredictions = predictionRepository
                .findCompletedPredictionsByParticipation(participation.getId());

        System.out.println("📊 Found " + completedPredictions.size() + " completed predictions");

        int totalPredictions = completedPredictions.size();
        int totalCorrect = 0;

        for (Prediction prediction : completedPredictions) {
            boolean isCorrect = isPredictionCorrect(prediction);
            prediction.setIsCorrect(isCorrect);

            if (isCorrect) {
                totalCorrect++;
            }

            if (prediction.getIsTiebreaker() && prediction.hasScorePrediction()) {
                prediction.calculateScoreDistance(
                        prediction.getMatch().getHomeScore(),
                        prediction.getMatch().getAwayScore()
                );
            }

            System.out.println("📌 Match: " + prediction.getMatch().getHomeTeam() + " vs " + prediction.getMatch().getAwayTeam() +
                    ", Predicted: " + prediction.getPredictedResult() +
                    ", Actual: " + prediction.getMatch().getHomeScore() + "-" + prediction.getMatch().getAwayScore() +
                    ", Correct: " + isCorrect);
        }

        participation.setTotalPredictions(totalPredictions);
        participation.setTotalCorrectPredictions(totalCorrect);

        double accuracy = totalPredictions == 0 ? 0.0 : (totalCorrect * 100.0) / totalPredictions;
        participation.setAccuracyPercentage(accuracy);
        participation.setHasCompletedAllPredictions(totalPredictions > 0);

        sessionParticipationRepository.save(participation);
        predictionRepository.saveAll(completedPredictions);

        System.out.println("✅ [END] Participation ID " + participationId +
                " updated: total=" + totalPredictions +
                ", correct=" + totalCorrect +
                ", accuracy=" + accuracy);
    }


    // 🎯 Check if main prediction is correct
    private boolean isPredictionCorrect(Prediction prediction) {
        Match match = prediction.getMatch();

        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            return false;
        }

        PredictionResult actualResult;
        if (match.getHomeScore() > match.getAwayScore()) {
            actualResult = PredictionResult.HOME_WIN;
        } else if (match.getHomeScore() < match.getAwayScore()) {
            actualResult = PredictionResult.AWAY_WIN;
        } else {
            actualResult = PredictionResult.DRAW;
        }

        return prediction.getPredictedResult() == actualResult;
    }

    // 📈 Update participation accuracy statistics
    private void updateParticipationAccuracy(SessionParticipation participation,
                                             List<Prediction> completedPredictions) {
        int totalPredictions = completedPredictions.size();
        int correctPredictions = (int) completedPredictions.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsCorrect()))
                .count();

        participation.setTotalPredictions(totalPredictions);
        participation.setTotalCorrectPredictions(correctPredictions);

        if (totalPredictions > 0) {
            double accuracy = (double) correctPredictions / totalPredictions * 100.0;
            participation.setAccuracyPercentage(accuracy);
        } else {
            participation.setAccuracyPercentage(0.0);
        }
    }

    // 🔍 Get user's predictions for a session
    public List<Prediction> getUserPredictionsForSession(Long participationId) {
        SessionParticipation participation = sessionParticipationRepository.findById(participationId)
                .orElseThrow(() -> new RuntimeException("Participation not found"));

        return predictionRepository.findByParticipation(participation);
    }

    // 📊 Get prediction summary by user and session
    public GameweekPreviewDTO getGameweekPreview(Long gameweekId,
                                                 SessionType sessionType,
                                                 BigDecimal buyInAmount,
                                                 LeagueTheme competition,
                                                 boolean isPrivate) {
        // 1. Get gameweek and validate it exists
        GameWeek gameWeek = gameweekRepository.findById(gameweekId)
                .orElseThrow(() -> new RuntimeException("GameWeek not found"));

        // 2. Get all matches for this gameweek
        List<Match> matches = matchRepository.findByGameweeksId(gameweekId);
        if (matches.isEmpty()) {
            throw new RuntimeException("No matches found for this gameweek");
        }

        // 3. Get tiebreaker matches (consistent selection)
        List<Long> tiebreakerMatchIds = generateTiebreakerMatches(gameweekId);

        // 4. Find session template (to show buy-in, max participants, etc.)
        SessionTemplate template = sessionTemplateRepository
                .findByCompetitionAndSessionTypeAndIsActiveTrue(competition, sessionType)
                .stream()
                .filter(t -> t.getBuyInAmount().equals(buyInAmount) && t.getIsPrivate().equals(isPrivate))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active template found"));

        // 5. Create preview DTO using constructor (not builder)
        GameweekPreviewDTO preview = new GameweekPreviewDTO();
        preview.setGameweekId(gameweekId);
        preview.setGameweekName("Gameweek " + gameweekId); // Simple fallback

        preview.setCompetition(competition);
        preview.setSessionType(sessionType);
        preview.setBuyInAmount(buyInAmount);
        preview.setMaxParticipants(template.getMaxParticipants());
        preview.setJoinDeadline(gameWeek.getJoinDeadline());
        preview.setMatches(matches);
        preview.setTiebreakerMatchIds(tiebreakerMatchIds);
        preview.setPrivate(isPrivate);
        preview.setSessionDescription(template.getTemplateName() + " - " + buyInAmount + " entry");

        return preview;
    }
    public double getTiebreakerScore(Long participationId) {
        Double score = predictionRepository.getTiebreakerScore(participationId);
        return score != null ? score : 999999.0; // High penalty if no tiebreaker predictions
    }
    @Transactional
    public void determineWinnersForGameWeek(Long gameweekId) {
        // 1. Get all participations related to this GameWeek
        List<SessionParticipation> participations = getSessionParticipationsByGameWeek(gameweekId);

        // 2. Group participations by session
        Map<Long, List<SessionParticipation>> groupedBySession = participations.stream()
                .collect(Collectors.groupingBy(sp -> sp.getSession().getId()));

        // 3. For each session, find the winner(s)
        for (List<SessionParticipation> sessionParticipations : groupedBySession.values()) {
            // Sort by accuracy DESC, then tiebreaker score ASC
            sessionParticipations.sort(Comparator
                    .comparingDouble(SessionParticipation::getAccuracyPercentage).reversed()
                    .thenComparingDouble(sp -> getTiebreakerScore(sp.getId()))
            );

            if (!sessionParticipations.isEmpty()) {
                // Mark the first participant as winner
                SessionParticipation winner = sessionParticipations.get(0);
                winner.setIsWinner(true);
                sessionParticipationRepository.save(winner);

                // Reset any previous winners (in case they exist) except this one
                sessionParticipations.stream()
                        .filter(sp -> !sp.equals(winner) && Boolean.TRUE.equals(sp.getIsWinner()))
                        .forEach(sp -> {
                            sp.setIsWinner(false);
                            sessionParticipationRepository.save(sp);
                        });
            }
        }
    }

    public List<SessionParticipation> getSessionParticipationsByGameWeek(Long gameweekId) {
        return sessionParticipationRepository.findByGameweekId(gameweekId);
    }
    @Transactional
    public void finalizeGameweekAfterCompletion(Long gameweekId) {
        List<SessionParticipation> participations = sessionParticipationRepository.findByGameweekId(gameweekId);

        // Step 1: Calculate accuracy for all participations
        for (SessionParticipation participation : participations) {
            calculatePredictionAccuracy(participation.getId());
        }

        // Step 2: Determine winners for each session in this gameweek
        determineWinnersForGameWeek(gameweekId);
    }

}