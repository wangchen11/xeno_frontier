#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <time.h>

void testArray(int size) {
    int bytes = size * 4;
    int* array = malloc(bytes);

    for(int i = 0; i < size; i++) {
        array[i] = i + 1;
        array[i] = i + 2;
        array[i] = i + 3;
        array[i] = i + 4;
        array[i] = i + 5;
        array[i] = i + 6;
        array[i] = i + 7;
        array[i] = i + 8;
        array[i] = i;
    }
    for(int i = 0; i < size; i++) {
        if (array[i] != i) {
            printf("c put and get not match");
            exit(-1);
        }
    }
    free(array);
}

int main() {
    int testArraySize = 10 * 1024 * 1024;
    struct timeval start;
    struct timeval end;
    
    testArray(testArraySize);

    for(int i = 0; i < 10; i++) {
        gettimeofday(&start, NULL);
        testArray(testArraySize);
        gettimeofday(&end, NULL);
        printf("%ld ms\n", (end.tv_usec - start.tv_usec) / 1000 + (end.tv_sec - start.tv_sec) * 1000);
    }
    return 0;
}
