package edu.rice.comp322;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Loads the contributors using the GitHub service.
 */
public interface LoadContributors {

    /**
     * Performs requests to GitHub and loads and aggregates the contributors for all
     * repositories under the given organization.
     */
    default int loadContributorsSeq(String username, String password, String org)
        throws IOException {

        //Create the service to make the requests
        GitHubService service = createGitHubService(username, password);

        //Get all the repos under the given organization
        List<Repo> repos = service.getOrgReposCall(org).execute().body();
        if (repos == null) {
            System.err.println("Error making request to GitHub. Make sure token and organization name are correct.");
            return 0;
        } else if (repos.size() == 0) {
            System.out.println("0 repositories found in " + org + " organization, make sure your token is correct.");
        } else {
            System.out.println("Found " + repos.size() + " repositories in " + org + " organization.");
        }

        //Get the contributors for each repo
        List<User> users = new ArrayList<>();
        for (Repo repo : repos) {
            List<User> tempUsers = service.getRepContributorsCall(org, repo.name).execute().body();
            if (tempUsers != null) {
                users.addAll(tempUsers);
                System.out.println("Found " + tempUsers.size() + " users in " + repo.name + " repository.");
            } else {
                System.err.println("Error making request to GitHub for repository " + repo.name);
            }
        }

        //Aggregate the number of contributions for each user
        System.out.println("Aggregating Results");
        List<User> aggregatedUsers = new ArrayList<>();
        for (User user: users) {
            if (aggregatedUsers.contains(user)) {
                aggregatedUsers.get(aggregatedUsers.indexOf(user)).contributions += user.contributions;
            } else {
                aggregatedUsers.add(user);
            }
        }

        //Sort the users in descending order of contributions
        aggregatedUsers.sort((o1, o2) -> o2.contributions - o1.contributions);
        System.out.println("Displaying Results");
        updateContributors(aggregatedUsers);
        return repos.size();
    }

    /**
     * Performs requests to GitHub and loads and aggregates the contributors for all
     * repositories under the given organization in parallel.
     */
    default int loadContributorsPar(String username, String password, String org)
        throws IOException {

        //TODO: implement parallel implementation
        System.err.println("Parallel Implementation not implemented");
        return 0;
    }

    /**
     * Creates the GitHub service with correct authorization.
     */
    default GitHubService createGitHubService(String username, String password) {
        String authToken = "Basic " + new String(Base64.getEncoder().encode((username + ":" + password).getBytes()), UTF_8);
        OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder().header("Accept", "application/vnd.github.v3+json").header("Authorization", authToken);
            Request request = builder.build();
            return chain.proceed(request);
        }  ).build();
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.github.com").addConverterFactory(
            GsonConverterFactory.create()).client(httpClient).build();
        return retrofit.create(GitHubService.class);
    }

    /**
     * Updates the contributors list displayed on the user-interface.
     * @param users a list of Users
     */
    void updateContributors(List<User> users);

}
