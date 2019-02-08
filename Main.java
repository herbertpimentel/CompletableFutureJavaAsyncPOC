import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {

    // Process async operation with partial erro handling 
    // https://medium.com/@senanayake.kalpa/fantastic-completablefuture-allof-and-how-to-handle-errors-27e8a97144a0

    static Random rand = new Random();

    private static int getRandomNumber(int max) {
        return Main.rand.nextInt(max) + 1;
    }

    // this is a task that receives parameters and returns a CompletableFuture
    private static CompletableFuture<Long> powerNum( ExecutorService executor, Long num) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                int time = (2000);
                Thread.sleep(time);
            } catch (Exception e) {

            }

            if (num <= 50L) {
                throw new IllegalArgumentException("You cannot power numbers below 50");
            }

            if (num <= 100L) {
                throw new RuntimeException("You cannot power numbers below 100");
            }            

            return num * num;
        }, executor).exceptionally( e -> {

            //Method handle additionally allows the stage to compute a replacement result that may enable further processing by other dependent stages. 
            //In all other cases, if a stage's computation terminates abruptly with an (unchecked) exception or error, then all dependent stages requiring its completion complete exceptionally as well, 
            //with a CompletionException holding the exception as its cause.            

            if (e.getCause() instanceof IllegalArgumentException) {
                System.out.println("Handle for IllegalArgumentException : " + e.getMessage());
            } else if (e.getCause() instanceof RuntimeException) {
                System.out.println("Handle for RuntimeException : " + e.getMessage());
            } else {
                System.out.println("Handle for Any expcetion : " + e.getMessage());
            }

            return null;
        });
    }

    public static void main(String[] args) throws Exception {
        
        List<Long>  list = new ArrayList<Long>();

        for (int i = 0; i < 1800; i++) {
            list.add(Long.valueOf(Main.getRandomNumber(1000)));
        }
       
        long tStart = System.currentTimeMillis();

        // clamp the amount of thread we can deal with
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Math.min(600, list.size())));

        // tansform each task in a individual CompletableFuture
        List<CompletableFuture<Long>> completableFutures = list.stream().map(num -> powerNum(executor, num)).collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]));

        // join every response in to a list to result it in the end
        CompletableFuture<List<Long>> allCompletableFuture = allFutures.thenApply(future -> {
            return completableFutures.stream()
                    .map(completableFuture -> completableFuture.join())
                    .collect(Collectors.toList());
        });

        // get all the results of tasks
        CompletableFuture completableFuture = allCompletableFuture.thenApply(longs -> {
            return longs.stream().map(n -> {
                if (n == null) { 
                    return null;
                }

                return n;
            }).collect(Collectors.toList());
        });


        List<Long> result = (List<Long>) completableFuture.get();

        long tEnd = System.currentTimeMillis();        

        System.out.println(result);

        double elapsedSeconds = ((tEnd - tStart) / 1000.0);
        System.out.println("Time: "+ elapsedSeconds + "s");

        System.exit(0);
    }
}