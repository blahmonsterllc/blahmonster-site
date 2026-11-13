# How Video Games Simulate Car Damage and Destruction (for a Demolition Derby Game)

> Research note on method: this session's network let web *search* through but blocked page *fetches* for nearly every relevant host (gameinformer.com, traxion.gg, gamingbolt.com, dev.siggraph.org, beamng.com, documentation.beamng.com, forums.unrealengine.com, 80.lv all failed DNS). Findings below therefore rest on search-result extracts of those pages, not full-page reads. Where a claim comes from forum posts, fan wikis or SEO-style sites it is flagged. Items in **Inferences** draw on general engineering and game-industry background knowledge and are not cited facts; the report writer should present them as design guidance, not reported fact.

## 1. How does BeamNG.drive's soft-body (node-and-beam) physics work, and what does it cost in CPU?

### Takeaway
BeamNG models every vehicle as a mass-spring-damper lattice: point masses ("nodes") linked by spring/damper "beams" that can permanently deform and break, with a visual mesh ("flexbody") skinned to the nodes. It steps physics at 2000 Hz (0.5 ms), so cost scales with node/beam count × 2000 steps/s. That makes it CPU-heavy (dozens of physics steps per rendered frame) and historically about one vehicle per CPU thread, which caps practical car counts far below a 20–24 car derby.

### Cited Findings
- Nodes are the core of BeamNG physics; everything is built around these mass points — [BeamNG JBeam wiki](https://wiki.beamng.com/JBeam_Reference)
- Beams are spring-and-damper links between nodes; spring rigidity is set in N/m (default 4,300,000) — [BeamNG docs: Beams](https://documentation.beamng.com/modding/vehicle/sections/beams/)
- `breakGroup`: beams in a group break together (type 0 breaks others in the group; type 1 is broken by the group but doesn't break others). This is how a hinge set lets a door or bumper come off as one unit — [BeamNG docs: Beams](https://documentation.beamng.com/modding/vehicle/sections/beams/); [JBeam Tips](https://documentation.beamng.com/modding/vehicle/intro_jbeam/jbeamtips)
- `deformGroup` is a trigger that fires once a beam's deformation passes a threshold (typically < 0.1). Its main uses are visual damage to glass and lights, and powertrain damage. This is how structural deformation drives gameplay damage states — [BeamNG docs: Beams / JBeam Tips](https://documentation.beamng.com/modding/vehicle/intro_jbeam/jbeamtips)
- Flexbodies are the visual meshes that deform with the node structure — [BeamNG docs: Flexbodies](https://documentation.beamng.com/modding/vehicle/sections/flexbodies/); [wiki](https://wiki.beamng.com/Flexbodies)
- Physics steps compute exactly 0.5 ms of simulated time by default, "often written as 2000Hz". The rate can be changed by launch argument, but 2000 Hz is the supported standard — [BeamNG docs: Tight Coupling](https://documentation.beamng.com/beamng_tech/tools/cosimulationeditor/tight_coupling/)
- Vehicle Lua can run per physics tick (`onPhysicsStep`, 2000 Hz) inside the per-vehicle VLUA virtual machine — [BeamNG docs: Virtual Machines](https://documentation.beamng.com/modding/programming/virtualmachines/)
- The game is CPU-bound because at 60 FPS the CPU must compute about 33 physics steps per frame (forum answer) — [BeamNG forum](https://www.beamng.com/threads/is-there-a-way-to-turn-up-physics.51381/)
- Threading (dated forum posts, ~2015, unofficial): "1 thread per vehicle" / "1 vehicle per core", with Lua and other subsystems on one thread — [BeamNG forum](https://beamng.com/threads/increasing-or-changing-physics-accuraccy-for-powerful-machines.2155)
- Cost argued to be roughly proportional to node + beam count plus per-object overhead. The T75 truck has about 660 nodes without wheels. A modder estimated a practical ceiling of about 4,000 nodes and 8,000 beams per vehicle (all forum, unofficial) — [BeamNG forum](https://beamng.com/goto/post?id=10038); [forum](https://www.beamng.com/posts/146959/)
- A developer said static (node vs. terrain) collision workload rises as the vehicle moves, because new node positions must be rechecked — [BeamNG forum](https://www.beamng.com/goto/post?id=147016)
- deformGroup is also used for engine damage. BeamNG simulates engine thermals, such as coolant and oil temperature leading to overheating — [BeamNG wiki: Engine Thermal Simulation](https://wiki.beamng.com/Engine_Thermal_Simulation)
- Rigs of Rods is the open-source (GPL-3.0, C++) predecessor using the same mass-spring-damper soft-body concept, with its custom "Beam" engine. Some of its developers went on to build BeamNG.drive. Repo: github.com/RigsOfRods/rigs-of-rods — [GitHub org](https://github.com/rigsofrods); [Wikipedia](https://Www.wikipedia.org/wiki/Rigs_of_Rods); [OSS Blog](https://ossblog.org/rigs-of-rods)

### Inferences
- Rough cost model: per step you integrate N nodes and evaluate B beam forces, plus node-vs-world and node-vs-triangle collision. At 2000 Hz and about 1,000 nodes / 4,000 beams, that is millions of beam evaluations per second per car. A 16–24-car derby with full BeamNG fidelity is unrealistic on consumer CPUs. BeamNG players commonly report only a handful of AI cars before slowdown; this is general community knowledge, unverified here.
- The high step rate exists because stiff springs (MN/m) need tiny timesteps for explicit integration to stay stable. A small team building its own node-beam system can use far fewer nodes (about 50–150 for a cage), softer beams and a lower rate (240–500 Hz with substeps), or implicit/PBD-style constraints (XPBD), to make soft-body derby cars affordable.
- The useful idea to copy from BeamNG even without soft-body physics: separate a coarse physical damage skeleton from the render mesh, then drive the render mesh (skinning or vertex offsets) from it. Use threshold triggers (deformGroup-like) to switch gameplay states such as lights broken, radiator holed or engine dead.

### Gaps
- No official, current (2023–2026) BeamNG figure for per-vehicle CPU cost or the threading model. All per-core claims are dated forum posts. The beamng.com "About physics" page could not be fetched.

## 2. How do specific titles handle damage? (Wreckfest/Next Car Game, FlatOut, Burnout, Destruction AllStars, Destruction Derby, GTA/Forza/GT/Dirt)

### Takeaway
There is a spectrum. At one end is full soft-body (BeamNG). Next is a "soft-body-lite" chassis plus detachable rigid parts (Wreckfest/ROMU). Then pre-split deformable panels plus hinged and detachable parts with zone health (FlatOut, Destruction AllStars). Then arcade crumple with scripted crash states (Burnout). At the far end, licensed sims keep damage restrained, mostly for licensing reasons. Derby-focused games all couple visual damage to mechanical damage (handling pull, engine failure, wheel loss) and to an elimination rule.

### Cited Findings
**Wreckfest / Next Car Game (Bugbear, ROMU engine)**
- Bugbear's in-house ROMU engine handles bending and buckling of the chassis, while attached parts detach more easily (and also deform meanwhile). Bugbear has used ROMU since about 2000. Source is a fan page, moderate reliability — [shapes.inc Wreckfest deep dive (fan)](https://shapes.inc/fandom/wreckfest/deep-dive)
- Bugbear's behind-the-scenes post for Next Car Game confirmed the damage is soft-body deformation, "as opposed to rigid bodies and pre-set damage models" — [bsimracing](https://www.bsimracing.com/game-spotting-behind-the-scenes-of-next-car-game-car-damage-fun/); [GamingBolt](https://gamingbolt.com/bugbear-entertainment-reveals-how-damage-modeling-works-in-next-car-game)
- Described as a "soft body deformation system that allows you to bend, buckle and dismember your car beyond recognition", reflecting how the car was actually abused "instead of pre-programmed, pre-determined damage models" — [GameSkinny](https://www.gameskinny.com/news/bugbear-inc-crowdfunding-next-car-game-utilizes-interesting-soft-body-car-damage/)
- Next Car Game was announced 27 Aug 2013 and later renamed Wreckfest — [Gameopedia wiki](https://gameopedia.fandom.com/wiki/Next_Car_Game:_Wreckfest)
- Early-access updates added a health/damage system and damage-status indicators next to player names, so players could pick targets they could beat. A later update showed damage dealt in red on the opponent's name tag — [DSOGaming](https://www.dsogaming.com/news/next-car-game-new-update-adds-new-tracks-introduces-new-healthdamage-system/); [DSOGaming](https://www.dsogaming.com/news/next-car-game-wreckfest-new-update-improves-cpu-performance-damage-system)
- Wreckfest 1 has "Normal" and "Realistic" damage modes. Realistic damage affects components like suspension and steering more severely, and one patch added over-rev engine damage in Realistic mode when downshifting too early — [search extract of Steam patch notes via steamdb](https://steamdb.info/patchnotes/18441968)
- Wreckfest 2 (Early Access from 20 Mar 2025): the Steam page promises "even higher crash fidelity, a deeper armor and damage system, more intricate component damage simulation" and an "Enhanced damage & tire model" — [Steam](https://store.steampowered.com/app/1203190/Wreckfest_2); [HostHavoc](https://hosthavoc.com/blog/wreckfest-2-early-access-launch)
- Wreckfest 2 chain reaction: heavy frontal hits damage the radiator, which leaks coolant and eventually overheats the engine until it blows a gasket — [PC Gamer](https://www.pcgamer.com/games/racing/theres-barely-more-than-a-demo-in-wreckfest-2s-early-access-debut-but-i-cant-stop-playing-it)
- Wreckfest 2 lets players score mechanical hits by targeting wheels, engine or radiator. Hits either increment a damage state or are terminal, such as engine destroyed — search extract, [traxion.gg](https://traxion.gg/?p=105753)
- A later update refined the damage model so cars last longer in Normal mode and Realistic is "more realistic". Early-access reviewers felt catastrophic damage came too easily, with one heavy hit able to end a race — [PC Gamer](https://www.pcgamer.com/games/racing/wreckfest-2s-latest-update-lets-you-drive-a-grotty-motorhome-thats-definitely-had-meth-cooked-in-it-and-ensures-that-realistic-damage-mode-is-now-more-realistic); [Xbox Tavern](https://www.xboxtavern.com/wreckfest-2-early-access-impressions-pc/); [Steam discussion](https://steamcommunity.com/app/1203190/discussions/0/603025512082741782)
- Wreckfest 2 updates added ragdoll physics — [traxion.gg](https://traxion.gg/wreckfest-2-update-adds-ragdoll-physics-three-new-cars-and-car-tuning/)

**FlatOut 1/2/Ultimate Carnage (Bugbear)**
- Reviews cite about 40 deformable parts or body panels per car in FlatOut 2, with dents and ripped-off doors, hoods and other parts — [Softpedia review](https://www.softpedia.com/reviews/games/playstation/FlatOut-2-55703.shtml); [games.gg](https://games.gg/flatout-2/)
- Progressive damage scales with impact intensity. Low-integrity cars catch fire and shed parts including wheels, crippling handling (user review / secondary) — [GameSpot user review](https://www.gamespot.com/flatout-2/user-reviews/2200-466644/); [Modstalgia](https://modstalgia.com/blog/10-reasons-why-flatout-2-is-still-the-best-arcade-racer)
- FlatOut 1's signature: on a hard enough impact the driver is thrown through the windshield as a ragdoll. FlatOut 2 turned this into ragdoll minigames (high jump, soccer, curling and others) — [Fun-Motion](https://www.fun-motion.com/?p=79)
- Wreckfest's dirt-track handling is described as more demanding than "the more forgiving FlatOut physics model" — [shapes.inc (fan)](https://shapes.inc/fandom/wreckfest/deep-dive)

**Burnout (Criterion)**
- Alex Ward: "it's all about how many bones are in the car... in the last games we made we were able to have 12 or 15 pieces of metal come out of the car." This suggests skeletal/bone-driven deformation plus a limited set of detachable pieces — [GamesRadar, Burnout 5 damage details](https://gamesradar.com/burnout-5-damage-details)
- Burnout Paradise has two crash types depending on the car's state afterwards. The car is "wrecked" if it loses any wheels, takes too much engine damage, lands on its side or roof, or leaves the map — [Wikipedia](https://en.wikipedia.org/wiki/Burnout_Paradise)
- Prerelease Paradise builds had far more severe deformation and more dynamic crash physics than the final game. Burnout 3 prerelease shows "crumpling akin to Burnout Revenge" versus the final game's "bending/stretching" — [TCRF Paradise](https://tcrf.net/Prerelease:Burnout_Paradise); [TCRF Burnout 3](https://www.tcrf.net/Prerelease:Burnout_3:_Takedown)
- NFS Hot Pursuit (2010), built on Paradise tech, had less structural deformation than the Burnout games (licensing context) — [GamesRadar](https://www.gamesradar.com/need-for-speed-hot-pursuit-could-redefine-online-racing-forever/2)

**Destruction AllStars (Lucid Games, PS5, 2021)**
- Each vehicle has about 200 individual parts that can interact with the world. Parts rattle, flap and detach on impact — [PlayStation Blog](https://blog.playstation.com/?p=347606); [GamesRadar](https://www.gamesradar.com/ps5-exclusive-destruction-allstars-car-smashing-and-character-abilities-detailed)
- Handling degrades with damage, but designers ramped it only in the final stretch of health: cars pull to one side especially if a wheel is lost or damage is one-sided. They deliberately avoided making cars so bad players abandon them — [Game Informer dev interview](https://www.gameinformer.com/2021/02/01/devs-share-a-behind-the-crash-look-at-destruction-allstars)
- DualSense adaptive triggers resist more as the car gets more damaged — [GTPlanet](https://www.gtplanet.net/?p=109081)
- Cars can't be repaired. Vehicle health is separate from the on-foot character's health — [GamePressure guide](https://www.gamepressure.com/destruction-allstars/repairing-a-car-is-it-possible/z5e394)
- A patch note mentions "wheel health zones" realigned to correct locations to improve wheel detachment. This confirms per-zone health hitboxes drive part loss — [traxion.gg patch coverage](https://traxion.gg/destruction-allstars-update-extends-season-one-and-improves-server-sync/)
- Lucid said PS5 let them make something that "felt real and wasn't just smoke and mirrors" — [PSU](https://www.psu.com/news/destruction-allstars-team-says-ps5-allowed-them-to-make-an-experience-that-felt-real-and-wasnt-just-smoke-and-mirrors/)

**Destruction Derby (Reflections, PS1, 1995) — the genre's template**
- Front hits can blow the radiator, and heavier front damage eventually disables the engine, ending your race. A HUD indicator shows ten damage locations. Severe damage makes the car harder to handle. No mid-race repairs (fan wiki) — [Destruction Derby wiki](https://destructionderby.fandom.com/wiki/Destruction_Derby?oldid=7926)
- Scoring: spinning an opponent 90°/180°/360° earns 2/4/10 points, doubled when spinning the race leader in Wreckin' Racing — [Newcastle Univ. fan page](https://www.staff.ncl.ac.uk/d.g.alsop/dd.htm); [wiki](https://destructionderby.fandom.com/wiki/Destruction_Derby?oldid=6814)

**Licensed games (GTA/Forza/Gran Turismo/GRID)**
- Damage levels are negotiated per manufacturer. "Two different vehicles by two different car manufacturers in the same game... have to have different damage levels" (developer to Jalopnik) — [Jalopnik](https://jalopnik.com/why-car-damage-sucks-in-racing-games-developer-explains-1850060288)
- GRID's game director: manufacturers "don't say no to damage, they just say 'Is it realistic damage?'" — [traxion.gg](https://traxion.gg/damage-inc-the-problem-with-crashes-in-racing-games)
- Forum claim, unverified: a common stipulation is that the car must remain drivable — [GTPlanet forum](https://www.gtplanet.net/forum/threads/granturismo-7-damage-model.396470/page-2)
- Many games use fictional brands to escape damage approvals, as GTA does with lookalike cars — [racinggames.gg (SEO-style)](https://racinggames.gg/article/the-real-reason-racing-games-ditch-licensed-cars-for-fictional-brands)
- GTA V's vehicle paint shader uses a dirt map on a second UV set (modding docs) — [Sollumz docs](https://docs.sollumz.org/documentation/fragments.yft/vehicle-shaders)

### Inferences
- For an indie derby game, **use fictional cars**. That frees you from licensing limits on damage, which Bugbear, FlatOut, Destruction AllStars and GTA all do.
- The common shipped recipe is: a deformable shell (procedural vertex offsets or bone-driven), N detachable panels with hinge-then-break behaviour, per-zone HP feeding mechanical systems, and a terminal "wrecked" rule. Burnout's "bones" quote and Destruction AllStars' "wheel health zones" both point to zone-based rather than fully simulated systems in arcade titles.
- Background knowledge, not verified here: Forza Motorsport and Gran Turismo 7 use limited cosmetic deformation, scratches and mechanical damage toggles, with mechanical damage often "off"/"simulation" settings. Dirt Rally uses panel-zone damage with mechanical failures (radiator, suspension, gearbox) and a service-park repair-time mechanic. GTA IV/V use real-time vertex deformation of the body plus detachable doors, bumpers and hoods. Treat these as unverified.

### Gaps
- No primary Bugbear GDC talk or engineering write-up on ROMU internals was found. The dev.siggraph.org "Perfecting destruction" article and the GamingBolt article could not be fetched.
- No primary source found on FlatOut's exact implementation (pre-split panel meshes vs. vertex deformation) or Burnout Takedown's crash-cam/takedown damage tech. Forza, Gran Turismo, GTA and Dirt damage specifics were not confirmed from primary sources.

## 3. Common techniques: deformation, mesh swaps, detachable parts, zone health, mechanical damage, particles, decals/damage textures

### Takeaway
Most shipped games combine several cheap layers rather than one expensive simulation:
- localized vertex displacement (or bone/lattice deformation) at impact
- hinged-then-breakable panels as separate rigid bodies
- per-zone HP mapped to mechanical penalties
- shader damage masks (vertex color or texture) blending paint into scratched metal, dirt or rust
- particles (sparks, glass, smoke whose density or color tracks health)

### Cited Findings
- **Vertex deformation at impact:** the common pattern is to take vertices within a radius of the contact point, push them along the impact direction with smooth falloff scaled by impact speed, and cap total deformation. This is suggested in Unity and Blender community threads; a Unity forum project "Mesh deformation on impact" ships source — [Unity Discussions](https://discussions.unity.com/t/now-with-source-mesh-deformation-on-impact/414788); [BlenderArtists](https://blenderartists.org/t/real-time-physics-based-mesh-deformation/645678)
- A Blender forum tip: "multiple levels of easy deformation on the car might even look better than hard way ones" — [BlenderArtists](https://blenderartists.org/t/real-time-physics-based-mesh-deformation/645678)
- **Shader-side deformation:** a three.js demo does real-time deformation in the vertex shader with distance falloff and smoothstep easing (metaball-style, not a car, but transferable to dents) — [GitHub benschg/three-shader-deform](https://github.com/benschg/three-shader-deform)
- **Academic game-oriented method:** a paper proposes "texture-vertex mapping" deformation that skips physical properties but gives cheap deformation feedback for real-time games — [arXiv 2304.05045 (ar5iv)](https://ar5iv.labs.arxiv.org/html/2304.05045)
- **Pre-baked deformation (Unreal):** "Easy Static Mesh Deformation Component" (Debugging Delight Studios) uses an editor tool to prepare the mesh without hand-made morph targets. It integrates with Chaos Vehicle and Advanced Vehicle System and supports engine damage, localized hits, protected passenger areas and detachable parts (v2.0–2.3.2 listed) — [Unreal forum](https://forums.unrealengine.com/t/2476292); [Fab listing](https://www.fab.com/listings/1c629d9e-044e-439b-873c-0b27f8e65902?lang=en)
- **Lattice deformation (Unreal):** "Crushform" puts a lattice around the body that takes each impact. Mesh, paint, glass, collision and hinged parts respond; doors, hood and trunk swing loose and tear off. You add one adapter component to a Chaos Vehicle. It depends on the Chaos Vehicles plugin (Experimental) — [Unreal forum](https://forums.unrealengine.com/t/dmitriy-vergasov-crushform-vehicle-damage/2847033)
- **Detach via break groups:** BeamNG's breakGroup lets all hinge beams of a part break together, and deformGroup fires gameplay effects like broken glass or lights and powertrain damage — [BeamNG docs](https://documentation.beamng.com/modding/vehicle/intro_jbeam/jbeamtips)
- **Zone health → part loss:** Destruction AllStars' "wheel health zones" drive wheel detachment — [traxion.gg](https://traxion.gg/destruction-allstars-update-extends-season-one-and-improves-server-sync/)
- **Mechanical damage:**
  - radiator → coolant leak → overheating → blown gasket (Wreckfest 2) — [PC Gamer](https://www.pcgamer.com/games/racing/theres-barely-more-than-a-demo-in-wreckfest-2s-early-access-debut-but-i-cant-stop-playing-it)
  - steering pull when damage is one-sided or a wheel is lost, only in the last stretch of health (Destruction AllStars) — [Game Informer](https://www.gameinformer.com/2021/02/01/devs-share-a-behind-the-crash-look-at-destruction-allstars)
  - suspension and steering damage in Realistic mode, over-rev engine damage (Wreckfest) — [steamdb](https://steamdb.info/patchnotes/18441968)
- **Damage textures:** store a damage weight per vertex in a vertex-color channel and blend clean paint into a damaged material (scratches, chipping, rust). Separate R/G/B channels can drive dirt, wear and scratches. Vertex resolution is coarse, so fine scratches need a texture mask — [StraySpark blog](https://www.strayspark.studio/blog/blender-vertex-colors-game-engine-workflows); [StraySpark procedural damage docs](https://www.strayspark.studio/docs/procedural-damage)
- Crashday (2006) ships a separate damage texture per body texture, with alpha controlling scratches — [Crashday SDK wiki](https://crashday.fandom.com/wiki/SDK_-_Texturing_guidelines_(.tga))
- BeamNG's "Grit" mod layers dynamic dirt, scratches and cracks per vehicle. Light scratches read poorly on white or chrome and well on dark paint — [BeamNG resources](https://beamng.com/resources/grit-%E2%80%93-dynamic-dirt-scratches-cracks-for-every-vehicle.40240/)
- Assetto Corsa's Custom Shaders Patch documents deforming bonnets as a mod-level technique — [CSP docs](https://cup.acstuff.club/docs/csp/cars/deforming-bonnets)
- **Pre-made damaged meshes:** the Unity Asset Store sells pre-damaged models ("Damaged SUV", "Low Poly Destructible Cars" with swap-in damaged parts). This is the mesh-swap approach — [Unity Asset Store](https://assetstore.unity.com/packages/3d/vehicles/low-poly-destructible-cars-44832); [Damaged SUV](https://assetstore.unity.com/packages/3d/vehicles/land/damaged-suv-167133)

### Inferences
- **Recommended layered stack for a small team:**
  1. **Body:** CPU vertex displacement on contact (radius falloff along the contact normal, clamped per vertex, written to a vertex buffer). Or deform a coarse lattice/cage (about 50–200 control points) and skin the render mesh to it. This is cheaper, smoother, network-friendly and avoids tearing between submeshes.
  2. **Panels:** doors, hood, trunk and bumpers as separate rigidbodies on hinge joints with angle limits. On zone HP threshold A, release the latch (unlimit the hinge so it flaps). On threshold B, break the joint and spawn the debris with a despawn timer.
  3. **Zones:** about 6–10 HP zones (front, rear, L, R, each wheel, engine, roof). Map them to mechanics: front → engine power curve and radiator leak timer; per-wheel → steering pull bias, toe/camber offset, then wheel detach; rear → fuel/fire risk. Ramp penalties non-linearly, mostly in the last 30% (Destruction AllStars' lesson).
  4. **Shader:** a damage mask (vertex color or a small runtime render-texture splat at hit UVs) blending paint, then primer, then bare metal, then rust, plus dirt accumulation.
  5. **Particles:** sparks on metal scrape (scale with relative speed), glass shards on window-zone break, and engine smoke whose rate and darkness rise with engine HP loss. Steam for radiator, black for engine, fire near death.
- Blend shapes or morph targets per zone (pre-authored "dented front" at weights 0–1) are the cheapest and most art-directable path. They work well for web/mobile and are trivially networked (one float per zone). The tradeoff is repetitive-looking damage.
- Recompute normals and bounds after deformation (or approximate normals in the shader). Keep collision as a simplified convex set that is optionally shrunk per zone, rather than re-cooking a mesh collider every hit.

### Gaps
- No primary source found on how FlatOut or Burnout author damage meshes (pre-split vs. procedural). No published performance numbers for vertex-deformation vs. lattice approaches in shipped titles.

## 4. How do games communicate car health, and how is "wrecked/eliminated" determined in derby modes?

### Takeaway
Games combine a HUD damage diagram or bar, readable visual state (crumple, missing parts, smoke and fire), opponent health tags for target selection, and sometimes haptics. Elimination is usually a terminal mechanical condition (engine destroyed, too many wheels lost, total HP zero) or a "wrecked" state such as being stuck or flipped, rather than purely cosmetic deformation.

### Cited Findings
- Destruction Derby (1995): a bottom-right HUD showed ten damage locations; heavy front damage disables the engine and ends your race (fan wiki) — [Destruction Derby wiki](https://destructionderby.fandom.com/wiki/Destruction_Derby?oldid=7926)
- Wreckfest added damage-status indicators next to player names so players could choose targets they could beat, and later showed damage dealt in red on opponent tags. The HUD damage indicator can be toggled — [DSOGaming](https://www.dsogaming.com/news/next-car-game-new-update-adds-new-tracks-introduces-new-healthdamage-system/); [ludo.guide (third-party)](https://www.ludo.guide/guide/wreckfest/game-settings-customization-beyond-controls)
- Wreckfest 2: hits on wheels, engine or radiator either increment a damage state or are terminal (engine destroyed) — [traxion.gg extract](https://traxion.gg/?p=105753)
- Burnout Paradise "wrecked" conditions: lost wheels, excess engine damage, landing on side or roof, or leaving the map — [Wikipedia](https://en.wikipedia.org/wiki/Burnout_Paradise)
- Destruction AllStars: a vehicle health bar separate from the character's; adaptive-trigger resistance rises with damage; handling pull appears in the last stretch of health — [GamePressure](https://www.gamepressure.com/destruction-allstars/repairing-a-car-is-it-possible/z5e394); [GTPlanet](https://www.gtplanet.net/?p=109081); [Game Informer](https://www.gameinformer.com/2021/02/01/devs-share-a-behind-the-crash-look-at-destruction-allstars)
- Impact Deformable (Unity asset) tracks structural damage numerically so cars can stop working after a set percentage — [TiplayStudio docs](https://tiplaystudio.gitbook.io/tiplay-assets/impact-deformable)
- Wreckfest 2 tuning: early access made catastrophic damage too easy (one hit could end a race), and Bugbear later made cars last longer in Normal mode. Elimination thresholds need tuning for fun — [PC Gamer](https://www.pcgamer.com/games/racing/wreckfest-2s-latest-update-lets-you-drive-a-grotty-motorhome-thats-definitely-had-meth-cooked-in-it-and-ensures-that-realistic-damage-mode-is-now-more-realistic); [Xbox Tavern](https://www.xboxtavern.com/wreckfest-2-early-access-impressions-pc/)

### Inferences
- **Suggested derby elimination rules**, any of which counts:
  - (a) engine zone HP = 0, which kills the car
  - (b) two or more wheels detached
  - (c) "immobile" for N seconds (speed < threshold and no successful hits), modelled on real derbies' "must make an aggressive hit every X seconds" rule
  - (d) flipped or on its side for N seconds with no self-right
  - (e) out of bounds
  - Add a short "last chance" window (engine sputtering) so eliminations feel earned, and attribute the kill to the last attacker within a time window for scoring.
- **Communication layers:**
  - HUD car silhouette with per-zone color (green→yellow→red→black), plus a total HP bar
  - opponents' HP shown on name tags (Wreckfest)
  - escalating smoke (white steam = radiator; grey → black = engine; flames = critical)
  - engine audio misfires, a screen-edge vignette, controller rumble or adaptive triggers
  - hit markers or damage numbers for dealt damage, and kill-feed attribution

### Gaps
- No primary source found for Wreckfest's exact derby elimination rule (e.g., a "wrecked" timer) or its HUD layout. Smoke-color-to-health conventions are widely used but no citable design source was found.

## 5. Available plugins, tools and open-source projects (Unity, Unreal, Godot, three.js/web)

### Takeaway
Unreal has the most complete current (2025–26) off-the-shelf options. Fab plugins such as Crushform and Easy Static Mesh Deformation add deformation, hinged parts and replication on top of Chaos Vehicles. Unity has older but workable deformation assets plus soft-body plugins. Godot and three.js have no turnkey car-damage solution, so you write vertex/shader deformation yourself. Rigs of Rods (GPL) is the open-source soft-body reference.

### Cited Findings
- **Unreal / Crushform:** lattice-based deformation of mesh, paint, glass and collision; hinged doors, hood and trunk that swing loose and tear off; one adapter component on a Chaos Vehicle; server-authoritative damage with client-side prediction; depends on Chaos Vehicles (Experimental) — [Unreal forum](https://forums.unrealengine.com/t/dmitriy-vergasov-crushform-vehicle-damage/2847033)
- **Unreal / Easy Static Mesh Deformation Component / "Vehicle Damage System" (Fab):** editor-baked deformation data (no hand morph targets), works with Chaos Vehicle, Advanced Vehicle System, FGear and R-Tune, replicated, Blueprint integration. It is unclear whether the two listings are the same product — [Fab listing](https://www.fab.com/listings/1c629d9e-044e-439b-873c-0b27f8e65902?lang=en); [Unreal forum](https://forums.unrealengine.com/t/2476292)
- **Unity / Impact Deformable:** structural damage tracked numerically. It is old (last version Jul 2019, Unity 2017.4) — [Unity Asset Store fallback](https://assetstore-fallback.unity.com/packages/tools/physics/impact-deformable-4799); [docs](https://tiplaystudio.gitbook.io/tiplay-assets/impact-deformable)
- **Unity:** an impact-based arcade mesh deformation asset (package 311903, date unclear) — [Unity Asset Store](https://assetstore.unity.com/packages/slug/311903)
- **Unity / Truss Physics:** a CPU soft-body plugin (older release) — [Unity Discussions](https://discussions.unity.com/t/released-truss-physics-for-unity3d-soft-body-simulation-plugin/594567)
- **Unity:** pre-damaged and destructible car models (Low Poly Destructible Cars, Off-road truck with damage system) — [Asset Store](https://assetstore.unity.com/packages/3d/vehicles/low-poly-destructible-cars-44832); [Off-road truck](https://assetstore.unity.com/packages/3d/vehicles/off-road-truck-with-damage-system-217329)
- **Unity community:** "Mesh deformation on impact" with source — [Unity Discussions](https://discussions.unity.com/t/now-with-source-mesh-deformation-on-impact/414788)
- **Godot:** community threads note that you can move any vertex, but tuning it to look right is the hard part, and shaders are easier but awkward for crash animation. No turnkey plugin was found — [Godot forum](https://forum.godotengine.org/t/how-would-i-make-a-car-in-godot-deform-similar-to-a-real-car/58203); [Godot Q&A](https://ask.godotengine.org/100682/how-to-deform-mesh-car-when-it-collides-car-crash-simulation)
- **three.js:** the vertex-shader deformation demo (see section 3) — [GitHub](https://github.com/benschg/three-shader-deform)
- **Rigs of Rods:** GPL-3.0 C++ soft-body vehicle simulator; repo github.com/RigsOfRods/rigs-of-rods, with a dedicated server repo — [GitHub org](https://github.com/rigsofrods)
- **Roblox:** the community has built car mesh deformation (EditableMesh-era tutorials) — [Roblox DevForum](https://devforum.roblox.com/t/car-mesh-deformation/1488887)

### Inferences
- **Unreal:** Chaos Vehicles plus a Fab damage plugin is the fastest route to Wreckfest-lite results. Chaos Destruction (Geometry Collections) suits shattering props and arena objects, such as barrels, walls and fences, more than crumpling sheet metal. It fractures into rigid pieces and doesn't bend. Background knowledge, unverified here.
- **Unity:** WheelCollider or an asset vehicle controller, plus a custom Job/Burst vertex deformation pass (or a lattice) and ConfigurableJoint/HingeJoint panels with breakForce. Unity's Mesh API (`MeshDataArray`, `SetVertexBufferData`) allows efficient runtime vertex updates. Background knowledge.
- **Godot 4:** VehicleBody3D (or a custom raycast vehicle) plus `ArrayMesh`/`MeshDataTool` or a vertex shader with a dent-sphere uniform array. Use HingeJoint3D/Generic6DOFJoint3D for panels. Jolt became Godot 4.4's built-in optional 3D physics engine (background knowledge, unverified here), which improves joint stability. Godot's SoftBody3D is cloth-like and not suited to car shells.
- **three.js/web:** pair with Rapier (WASM) or cannon-es for the rigid-body vehicle (raycast vehicle) and joints. Do dents in the vertex shader with a uniform array of up to about 16–32 recent impacts (position, radius, depth), or on the CPU by editing `BufferGeometry` positions and setting `needsUpdate`, then recompute normals. Budget-wise, morph-target zones are the safest choice for many cars in a browser.
- **Soft-body for a small team:** a coarse node-beam cage (Rigs of Rods / BeamNG style) is feasible in C#, C++ or Rust for 8–12 cars if nodes stay under about 100 per car and you use PBD/XPBD distance constraints with plastic rest-length update (on overstrain, set rest length = current length). Treat this as an inference, not a measured benchmark.

### Gaps
- Could not verify 2025–2026 versions, prices or engine compatibility of the Unity and Unreal assets (fab.com and forums.unrealengine.com fetches blocked). No current Godot or three.js car-damage plugin was found. Whether the Unity Asset Store's "Realistic vehicle damage" system (80.lv article) is available could not be checked.

## 6. Multiplayer/networking considerations for deformation

### Takeaway
Full soft-body state is too large to sync, so multiplayer games sync *damage events or zone states* and let each client reproduce deformation locally ("close enough" visuals). The server stays authoritative over gameplay-relevant health, part detachment and elimination. BeamMP shows this works but stresses CPU, and commercial plugins advertise server-authoritative damage with client prediction.

### Cited Findings
- BeamMP (BeamNG multiplayer mod) synchronizes vehicles and their damage between all connected players — [EmeraldHost docs](https://docs.emeraldhost.de/en/gameserver/beammp)
- Community analysis: the problem is "the huge amount of node information" that everyone would need to send. Proposals use simplified collision bodies for remote cars with a visual model on top, and "it was and still is all interpolation, identical is not needed, just close enough" (forum theory) — [BeamNG forum: Multiplayer theory](https://www.beamng.com/threads/multiplayer-theory.51413/)
- BeamMP offers an experimental "simplified vehicles" option to cut processing load online. Players report heavy frame drops only when online, and desync of other players' cars — [BeamMP forum](https://forum.beammp.com/t/bmp-online-experiencing-heavy-frame-drops-only-online/405654); [BeamMP forum](https://forum.beammp.com/t/issue-with-other-peoples-cars/304131)
- BeamMP queues remote vehicle spawns and part changes — [BeamMP forum](https://forum.beammp.com/t/car-tuning-parts-sync-failed/1117252)
- Crushform (Unreal): "server-authoritative damage with client-side prediction" — [Unreal forum](https://forums.unrealengine.com/t/dmitriy-vergasov-crushform-vehicle-damage/2847033)
- Easy Static Mesh Deformation is marketed as "(Replicated)" — [Unreal forum](https://forums.unrealengine.com/t/2476292)
- Destruction AllStars shipped an update to "improve server sync" (patch coverage) — [traxion.gg](https://traxion.gg/destruction-allstars-update-extends-season-one-and-improves-server-sync/)

### Inferences
- **Recommended model:**
  - **Server authoritative:** zone HP, mechanical state (engine, radiator, wheel attached flags), panel states (latched / flapping / detached) and elimination. Replicate these as small enums or bytes, reliable on change.
  - **Deformation as events:** on each significant hit the server broadcasts {car id, local-space contact point, direction, magnitude, seed}. Every client applies the same deterministic deformation function, so meshes converge visually without streaming vertices. Late joiners get a compact snapshot: per-zone dent weights, or the list of the last N impacts merged into a lattice offset array of a few hundred bytes.
  - **Detached debris:** non-authoritative, client-local cosmetic rigid bodies, except wheels or panels that matter for gameplay as obstacles. Either replicate those few or make them non-colliding after a short time.
  - **Collision response:** car-vs-car hits are the hardest latency case in physics games. Use server-side physics with client prediction for your own car and interpolation for others, and attribute hits on the server. Don't let clients self-report damage, to stop cheating.
- Blend-shape/zone-weight damage is the most network-friendly option (one byte per zone). Lattice deformation is next (dozens to hundreds of floats, quantizable). Per-vertex or soft-body node state is the least friendly.

### Gaps
- No primary source on BeamMP's wire protocol or on how Wreckfest or Destruction AllStars network deformation. Not found in accessible sources; the BeamMP GitHub would be the next place to check.
