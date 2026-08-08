package in.ankush.cloudshareapi.service;


import in.ankush.cloudshareapi.document.UserCredits;
import in.ankush.cloudshareapi.repository.UserCreditsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreditsService {
    private  final UserCreditsRepository userCreditsRepository;
    private final  UserService userService;
    public UserCredits createInitialCredits(String userId){
        UserCredits userCredit = UserCredits.builder()
                .userId(userId)
                .credits(10)
                .plan("BASIC")
                .build();

        return  userCreditsRepository.save(userCredit);

    }

    public UserCredits getUserCredits(String userId){
        return userCreditsRepository
                .findByUserId(userId)
                .orElseGet(() -> createInitialCredits(userId));
    }

    public UserCredits getUserCredits(){
        String userId = userService.getCurrentUser().getId();
        return getUserCredits(userId);
    }
   public Boolean hashEnoughCredits(int requiredCredits){
        UserCredits userCredits = getUserCredits();
        return userCredits.getCredits() >= requiredCredits;
   }


   public UserCredits consumeCredit(){
        UserCredits userCredits = getUserCredits();

        if (userCredits.getCredits() <= 0){
            return null;
        }
        userCredits.setCredits(userCredits.getCredits() - 1);
        return userCreditsRepository.save(userCredits);
   }

   public UserCredits addCredits(String userId, Integer creditsToAdd, String plan){
      UserCredits userCredits =  userCreditsRepository.findByUserId(userId)
                .orElseGet(() -> createInitialCredits(userId));

      userCredits.setCredits(userCredits.getCredits() + creditsToAdd);
      userCredits.setPlan(plan);
      return userCreditsRepository.save(userCredits);
   }
}
