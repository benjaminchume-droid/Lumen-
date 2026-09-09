import { MediaItem } from "../types";

export const CURATED_CATALOG: MediaItem[] = [
  {
    id: "the-metamorphosis",
    title: "The Metamorphosis",
    originalTitle: "Die Verwandlung",
    type: "novel",
    author: "Franz Kafka",
    coverUrl: "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=600",
    description: "One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin. He lay on his armour-like back, and if he lifted his head a little he could see his brown belly, slightly domed and divided by arches into stiff sections.",
    genres: ["Existential", "Absurdist", "Classic Literature", "Psychological"],
    status: "completed",
    rating: 4.9,
    chaptersCount: 3,
    chapters: [
      {
        id: "chapter-1",
        title: "Part I: The Awakening",
        number: 1,
        uploadedAt: "2026-05-20",
        content: `### Gregor's Metamorphosis

One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin. He lay on his armour-like back, and if he lifted his head a little he could see his brown belly, slightly domed and divided by arches into stiff sections. The bedding was hardly able to cover it and seemed ready to slide off any moment. His many legs, pitifully thin compared with the size of the rest of him, waved helplessly before his eyes.

"What's happened to me?" he thought. It wasn't a dream. His room, a proper human room although a little too small, lay peacefully between its four familiar walls. A collection of textile samples lay spread out on the table—Sansa was a travelling salesman—and above it there hung a picture that he had recently cut out of an illustrated magazine and housed in a nice, gilded frame. It showed a lady fitted out with a fur hat and fur boa who sat upright, raising a heavy fur muff that covered the whole of her lower arm towards the viewer.

Gregor then turned to look out the window at the dull weather. Drops of rain could be heard hitting the pane, which made him feel quite sad. "How about if I sleep a little bit longer and forget all this nonsense," he thought, but that was something he was unable to do because he was used to sleeping on his right, and in his present state he couldn't get into that position. However hard he threw himself onto his right, he always rolled back to where he was. He must have tried it a hundred times, shut his eyes so that he wouldn't have to look at the floundering legs, and only stopped when he began to feel a mild, dull pain there that he had never felt before.

"Oh, God", he thought, "what a strenuous career it is that I've chosen! Travelling day in and day out..."`
      },
      {
        id: "chapter-2",
        title: "Part II: Domestic Quarantine",
        number: 2,
        uploadedAt: "2026-05-21",
        content: `### Adapting to the New Form

It was not until the twilight that Gregor awoke from his deep, sleep-like swoon. He would have woken up not much later anyway even without any disturbance, for he felt himself sufficiently rested and sleeping off his fatigue, but it seemed to him as if a hurried step and a cautious shutting of the door leading to the hall had awakened him.

The light of the electric streetlamps cast pale reflections here and there of the ceiling and of the higher parts of the furniture, but down where Gregor lay it was dark. Slowly, still awkward with his feelers, which he now first learned to estimate, he scrubbed his way to the door to see what had been happening. His left side seemed to be one single long, unpleasantly contracting scar, and he had to limp on his two rows of legs. One little leg, moreover, had been seriously injured in the course of the morning's occurrences—it was almost a miracle that only one had been hurt—and trailed listlessly behind.

At the door he first found out what had attracted him: it was the smell of something to eat. There stood a basin filled with sweet milk, in which swam tiny pieces of white bread. He was so pleased he almost laughed with joy, for he was even hungrier than he had been in the morning, and he immediately dipped his head into the milk almost up to his eyes.`
      }
    ]
  },
  {
    id: "the-time-machine",
    title: "The Time Machine",
    originalTitle: "Chronos Expedition",
    type: "manga",
    author: "H.G. Wells",
    artist: "Lumen Studio",
    coverUrl: "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&q=80&w=600",
    description: "An inventor's incredible voyage into the far future of humanity, where he discovers the divided Eloi and Morlock societies. Witnessed as a sequence of high-contrast, dramatic manga storyboard panels with immersive cinematic layouts.",
    genres: ["Sci-Fi", "Manga Adapt", "Dystopian", "Adventure"],
    status: "completed",
    rating: 4.8,
    chaptersCount: 1,
    chapters: [
      {
        id: "chapter-1",
        title: "The Fourth Dimension",
        number: 1,
        uploadedAt: "2026-05-25",
        mangaPanels: [
          {
            id: "panel-1",
            visualConcept: "A highly detailed, intricate golden brass dial with glowing crystalline conduits. Refracted rays of light burst from the dial, illuminating a dimly lit Victorian workshop.",
            layoutType: "hero",
            atmosphericColor: "#1d3557",
            dialogues: [
              {
                character: "The Time Traveller",
                text: "Scientific minds know very well that Time is only a kind of Space...",
                positionX: 15,
                positionY: 20
              },
              {
                character: "The Philosopher",
                text: "But how can we move in it if it is space? We cannot escape our present moment!",
                positionX: 75,
                positionY: 70
              }
            ]
          },
          {
            id: "panel-2",
            visualConcept: "A extreme close-up of a hand clutching a cold ivory lever, hovering over a polished brass frame with miniature mechanical gears rotating rapidly.",
            layoutType: "split-vertical",
            atmosphericColor: "#a8dadc",
            dialogues: [
              {
                character: "The Time Traveller",
                text: "This little lever, when pressed... will slide me into the future.",
                positionX: 30,
                positionY: 15
              }
            ],
            narrativeText: "The air grows heavy with electric tension."
          },
          {
            id: "panel-3",
            visualConcept: "The workshop window. The sun moves like an incandescent arc through the heavens; shadows spin across the lawn like a whirling strobe light.",
            layoutType: "cinematic",
            atmosphericColor: "#f1faee",
            dialogues: [],
            narrativeText: "Night followed day like the flapping of a black wing. The laboratory spun into a blur."
          },
          {
            id: "panel-4",
            visualConcept: "A grand colossal white marble statue shaped like a winged Sphinx, weathered and stained with moss under a violet, storm-laden sky.",
            layoutType: "full-width",
            atmosphericColor: "#457b9d",
            dialogues: [
              {
                character: "The Time Traveller",
                text: "Year 802,701 AD... What has become of our world?",
                positionX: 20,
                positionY: 80
              }
            ]
          }
        ]
      }
    ]
  },
  {
    id: "cosmic-reverie",
    title: "Cosmic Reverie",
    originalTitle: "Space Dust Anthology",
    type: "hybrid",
    author: "Lumen Glassworks",
    coverUrl: "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80&w=600",
    description: "A premium hybrid reading experience, integrating floating descriptive splash screens and rich, elegant, paragraph-by-paragraph cinematic soundscapes to illustrate the cold silence of space exploration.",
    genres: ["Cosmic", "Hybrid Literary", "Arthouse"],
    status: "ongoing",
    rating: 4.7,
    chaptersCount: 1,
    chapters: [
      {
        id: "chapter-1",
        title: "Echoes of the Cosmos",
        number: 1,
        uploadedAt: "2026-05-26",
        hybridElements: [
          {
            id: "elem-1",
            type: "cinematic-splash",
            content: "A singular metallic probe drifting through a dense nebula of gas and crystalline dust, glowing soft sapphire and turquoise.",
            assetPrompt: "Deep space floating probe, bright neon turquoise gas tail, highly detailed, realistic cinema lens flare",
            atmosphericColor: "#001d3d"
          },
          {
            id: "elem-2",
            type: "text",
            content: "The signal came from the dark sector of the Orion Belt, not in a digital wave, but as a low, recurring sub-bass frequency. It vibrated directly through the composite alloy hull of our research vessel, bypassing standard receiver channels and rendering itself as a physical thrum in the soles of our boots.",
            focusHighlight: true
          },
          {
            id: "elem-3",
            type: "image-spread",
            content: "The command console screens flashing in warning, cast under the cold crimson alarm of secondary power reserves.",
            assetPrompt: "Sci-fi command bridge console blinking with crimson neon diagnostics, liquid glass screen panels refracting ambient warning light",
            atmosphericColor: "#3e0303"
          },
          {
            id: "elem-4",
            type: "overlay-narration",
            content: "We were never meant to listen to the spaces between the stars. But silence is an unstable medium."
          }
        ]
      }
    ]
  }
];
