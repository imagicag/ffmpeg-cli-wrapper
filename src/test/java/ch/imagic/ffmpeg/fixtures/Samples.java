package ch.imagic.ffmpeg.fixtures;

public final class Samples {
    private Samples() {
        throw new AssertionError("No instances for you!");
    }

    // Test sample files (only a handful to keep the repo small)
    public static final String TEST_PREFIX = "src/test/resources/ch/imagic/ffmpeg/samples/";

    public static final String BASE_BIG_BUCK_BUNNY_720P_1MB = "big_buck_bunny_720p_1mb.mp4";
    public static final String BASE_TESTSCREEN_JPG = "testscreen.jpg";
    public static final String BASE_TEST_MP3 = "test.mp3";
    public static final String BASE_VIDEO_WITH_RESERVED_COLOR_SPACE = "video_with_reserved_color_space.mp4";

    public static final String BIG_BUCK_BUNNY_720P_1MB = TEST_PREFIX + BASE_BIG_BUCK_BUNNY_720P_1MB;
    public static final String TESTSCREEN_JPG = TEST_PREFIX + BASE_TESTSCREEN_JPG;
    public static final String TEST_MP3 = TEST_PREFIX + BASE_TEST_MP3;
    public static final String VIDEO_WITH_RESERVED_COLOR_SPACE = TEST_PREFIX + BASE_VIDEO_WITH_RESERVED_COLOR_SPACE;

    private static final String BOOK_M4B = "book_with_chapters.m4b";
    public static final String BOOK_WITH_CHAPTERS = TEST_PREFIX + BOOK_M4B;
    private static final String BASE_SIDE_DATA_LIST = "side_data_list";
    public static final String SIDE_DATA_LIST = TEST_PREFIX + BASE_SIDE_DATA_LIST;

    // We don't have the following files
    public static final String FAKE_PREFIX = "fake/";

    public static final String ALWAYS_ON_MY_MIND = FAKE_PREFIX + "Always On My Mind [Program Only] - Adelén.mp4";

    public static final String START_PTS_TEST = FAKE_PREFIX + "start_pts_test_1mb.ts";

    public static final String DIVIDE_BY_ZERO = FAKE_PREFIX + "Divide By Zero.mp4";

    // TODO Change to a temp directory
    // TODO Generate random names, so we can run tests concurrently
    public static final String OUTPUT_MP4 = "output.mp4";
}
