package backend.domain;

public class ModInfoServiceTest {

    //TODO: This all needs refactored since the massive rework of the ModInfoService class.
//    ModlistRepository modlistRepository;
//    ModInfoService service;
//    List<Mod> modList;
//
//    String testPath;
//
//    String badExtensionPath;
//
//    Properties properties;
//
//    //TODO: Add test for incorrect file extension
//    @BeforeEach
//    void setup() throws IOException {
//        properties = new Properties();
//        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("SEMM_Test.properties")) {
//            properties.load(input);
//        }
//
//        modlistRepository = mock(ModlistFileRepository.class);
//        service = new ModInfoService(modlistRepository, properties);
//        modList = new ArrayList<>();
//        modList.add(new SteamMod("2777644246")); //Binoculars
//        modList.add(new SteamMod("2668820525")); //TouchScreenAPI
//        modList.add(new SteamMod("1902970975")); //Assertive Combat Systems
//        badExtensionPath = "src/test/resources/nomods.sbc";
//        testPath = "src/test/resources/GoodModList.txt";
//    }
//
//    @Test
//    void shouldGetModListWithThreeItems() throws IOException {
//        when(modlistRepository.getSteamModList(new File(testPath))).thenReturn(modList);
//
//        List<Mod> testModList = service.getModListFromFile(testPath).getPayload();
//        assertEquals(modList, testModList);
//        assertEquals(3, testModList.size());
//    }
//
//    @Test
//    void shouldCompleteSteamModListWithFriendlyNameAndServiceName() throws ExecutionException, InterruptedException, IOException {
//        for(Mod m : modList) {
//            service.generateModInformation(m);
//        }
//        assertEquals("Binoculars", modList.get(0).getFriendlyName());
//        assertEquals("TouchScreenAPI", modList.get(1).getFriendlyName());
//        assertEquals("Assertive Combat Systems", modList.get(2).getFriendlyName());
//
//        assertEquals("Steam", modList.get(0).getPublishedServiceName());
//        assertEquals("Steam", modList.get(1).getPublishedServiceName());
//        assertEquals("Steam", modList.get(2).getPublishedServiceName());
//    }
//
//    @Test
//    void shouldDownloadSteamInformationForAMod() throws ExecutionException, InterruptedException, IOException {
//        List<Mod> testModList = new ArrayList<>();
//        testModList.add(new SteamMod("3276848116")); //Maelstrom - Black Hole
//        service.generateModInformation(testModList.getFirst());
//        assertEquals("Maelstrom - Black Hole", testModList.getFirst().getFriendlyName());
//    }
//
//    @Test
//    void shouldAppendNotASteamModToModNameIfItIsNotAModItem() throws ExecutionException, InterruptedException, IOException {
//        List<Mod> testModList = new ArrayList<>();
//        testModList.add(new SteamMod("2396138200")); //Big Bird - Blueprint
//        testModList.add(new SteamMod("1653185489")); //Escape From Mars Wico [Update WIP] - World
//        service.generateModInformation(testModList.getFirst());
//        service.generateModInformation(testModList.getLast());
//        assertEquals("Big Bird_NOT_A_MOD", testModList.getFirst().getFriendlyName());
//        assertEquals("Escape From Mars Wico [Update WIP]_NOT_A_MOD", testModList.getLast().getFriendlyName());
//    }
//
//    @Test
//    void shouldGetFileDoesNotExist() throws IOException {
//        Result<List<Mod>> result = service.getModListFromFile("src/this/path/does/not/exist");
//        assertFalse(result.isSuccess());
//        assertEquals("File does not exist.", result.getCurrentMessage());
//    }
//
//    @Test
//    void shouldGetIncorrectFileExtension() throws IOException {
//        Result<List<Mod>> result = service.getModListFromFile(badExtensionPath);
//        assertFalse(result.isSuccess());
//        assertEquals("Incorrect file type selected. Please select a .txt or .doc file.", result.getCurrentMessage());
//    }
}
