// Source: https://gist.github.com/scivision/f582e5c24a3efbf414a7557dfe99a2fb

#include <stdio.h>
#include <stdlib.h>
#include <time.h>

int main(void) {
    // January 19, 2038, 03:14:08 UTC (just before the 2038 problem boundary)
    time_t y2038 = 0x7FFFFFFF;

    // One second after (after the problematic timestamp)
    time_t post_y2038 = y2038 + 1;

    struct tm* pre_time = gmtime(&y2038);
    struct tm* post_time = gmtime(&post_y2038);

    printf("Testing time_t size and Year 2038 compatibility:\n");
    printf("Size of time_t: %zu bytes\n", sizeof(time_t));

    printf("Time just before Y2038 problem: %d-%02d-%02d %02d:%02d:%02d\n",
           pre_time->tm_year + 1900, pre_time->tm_mon + 1, pre_time->tm_mday,
           pre_time->tm_hour, pre_time->tm_min, pre_time->tm_sec);

    printf("Time just after Y2038 problem: %d-%02d-%02d %02d:%02d:%02d\n",
           post_time->tm_year + 1900, post_time->tm_mon + 1, post_time->tm_mday,
           post_time->tm_hour, post_time->tm_min, post_time->tm_sec);

    // Check if the time correctly wraps to 2038
    if (post_time->tm_year + 1900 >= 2038){
        printf("PASS: time_t can handle dates after January 19, 2038\n");
        return EXIT_SUCCESS;
    }

    fprintf(stderr, "FAIL: time_t cannot handle dates after January 19, 2038\n");

    return EXIT_FAILURE;
}
