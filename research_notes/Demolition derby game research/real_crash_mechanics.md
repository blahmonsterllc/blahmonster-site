# Real-World Car Crash Mechanics (for a Demolition Derby Game)

Research note: page fetches were blocked by the session network (speedwaymotors.com, thedailyautomotive.com, journal.nafe.org and derbyparts.com would not resolve), so findings come from search-result content and excerpts. Several derby sources are vendor or forum material, which is practical driver knowledge rather than engineering data. Where a claim is my own inference or general physics, it is in an Inferences section, not a Cited Findings section.

## 1. How crumple zones work, and body-on-frame vs unibody deformation

### Takeaway
Modern cars are built as a stiff occupant "safety cage" with soft, engineered crush zones in front and behind. The crush zones fold progressively to stretch the stopping time and soak up energy. Older body-on-frame derby cars (1960s–90s full-size American sedans and wagons) have a separate rigid ladder frame that resists deformation. That makes them crumple less efficiently in a road crash but far more durable when hit over and over, which is why derby rules and drivers favour them.

### Cited Findings
- Crumple zones work by letting the car deform so the time over which the vehicle's speed drops to zero is longer, which lowers forces on occupants — [Motor1, "Why modern cars are designed to fall apart in a crash"](https://www.motor1.com/news/814631/why-cars-are-designed-to-crumple-in-a-crash/); [Philkotse](https://philkotse.com/safe-driving/crumple-zones-and-car-chassis-design-how-it-keeps-you-alive-2746)
- IIHS: most modern cars have safety cages around the occupant compartment, built to withstand head-on and moderate-overlap frontal crashes with little deformation. Crush zones manage crash energy to reduce forces on that compartment. Engineers judge a crash by measuring intrusion into the cabin at key points afterwards — [IIHS, About our tests](https://www.iihs.org/ratings/about-our-tests)
- In small-overlap crashes (the outer corner of the front), the crush zones are largely bypassed and "the safety cage needs to resist crash forces that aren't tempered by crush-zone structures" — [IIHS, About our tests](https://www.iihs.org/ratings/about-our-tests)
- When impact speed rose above design levels in IIHS-linked tests, kinetic energy exceeded what the energy-absorbing structures could handle. The excess went into the occupant compartment, and injury measures rose — [IIHS bibliography: Impact of speeds on drivers and vehicles](https://www.iihs.org/topics/bibliography/ref/2218)
- Frontal crash energy depends on test speed and vehicle weight, so results are not comparable across weight classes — [IIHS, About our tests](https://www.iihs.org/ratings/about-our-tests)
- In unibody cars the whole body is one load-bearing structure with engineered crumple zones. A separate ladder frame (body-on-frame) is rigid by design, resists deformation and is harder to engineer crumple zones into — [CarFromJapan](https://carfromjapan.com/article/body-on-frame-vs-unibody-construction); [RealTruck](https://realtruck.com/blog/unibody-vs-body-on-frame-whats-the-difference/); [CJ Pony Parts](https://www.cjponyparts.com/resources/body-on-frame-vs-unibody) (consumer/vendor explainers; they broadly agree)
- Collision-repair industry frame-damage taxonomy (useful as a deformation vocabulary):
  - **Mash:** the frame is shortened, "compress[ed] like an accordion", from head-on or rear hits. It can happen even at low speed.
  - **Sway:** sideways bend off the centerline. The frame "appears to lean to one side" from above.
  - **Sag:** the centre drops in height. Panel gaps wide at the bottom and narrow at the top are a visible clue.
  - **Diamond:** one rail is pushed back relative to the other, so the frame becomes a parallelogram. It causes "dog tracking", where the car drives crabwise.
  - **Twist:** one corner is higher or lower than the others.

  These types usually occur together ("sway, diamond and twist very often exist together"). Sources: [FenderBender, Advanced Structural Analysis](https://www.fenderbender.com/running-a-shop/operations/article/33019960/advanced-structural-analysis); [Cash Auto Salvage, What is frame damage](https://www.cashautosalvage.com/frame-damage/); [Caliber Collision](https://www.caliber.com/services/collision/collision-repair/car-frame-damage-101-detection-and-repair)
- On a full (separate) frame, diamond or twist is usually a fairly simple repair. On a unibody, either condition means the vehicle cannot be repaired — [FenderBender](https://www.fenderbender.com/running-a-shop/operations/article/33020417/entering-a-new-dimension) (search excerpt)
- Derby cars: competitors have traditionally used full-size American sedans and wagons, especially from the 1960s–70s, which had more robust frames than later vehicles — [Wikipedia, Demolition derby](https://en.wikipedia.org/wiki/Demolition_derby)
- The 1964–68 Chrysler Imperial is described as nearly unbreakable and banned at most derbies for its durability. Sources disagree slightly on the years (one says 1964–66). The Ford Panther platform (LTD, Crown Victoria, Mercury Grand Marquis, police versions especially) is the most-recommended legal car because of its full frame. The Chevy Impala, in both box and bubble body styles, is also commonly named — [SlashGear](https://www.slashgear.com/1799514/demolition-derby-cars-typically-used/); [MotorBiscuit](https://www.motorbiscuit.com/best-car-demolition-derby/); [Performance Plus Tire](https://www.performanceplustire.com/Blog/best-cars-for-demo-derby); [INDYCNC](https://indycnc.com/2024/08/top-10-legendary-demolition-derby-cars-icons-of-the-arena/)
- An informal fan poll (Lemons video): Crown Victoria 58%, Imperial 28% as the car to win a derby. This is not authoritative — [YouTube short](https://www.youtube.com/shorts/jyMnNEg4WYo)
- Unibody imports are usually restricted to compact heats. A New Jersey regulation bars full-frame vehicles from the compact class. Anecdotally, older rear-drive Volvo 240/740s do well among unibodies because the structure over the rear axle stays straight — [countyfairsusa.net](https://countyfairsusa.net/demolition-derby-rules-explained/); [N.J. Admin. Code 13:62-9.1](https://www.law.cornell.edu/regulations/new-jersey/N-J-A-C-13-62-9-1); [VWVortex thread](https://www.vwvortex.com/threads/the-demolition-derby.3437095/) (anecdotal)

### Inferences
- For a game, the two construction types map to two damage personalities:
  - **Body-on-frame:** the body panels (sheet metal) deform easily and visibly, but the frame is a separate tough "hit-point pool". Frame damage shows up as sway, sag, diamond or twist, which change handling (dog-tracking, pulling to one side) rather than destroying the car outright. Good for many repeated hits.
  - **Unibody:** the crush zones absorb the first big hit very well, with lots of visible crumple, but each zone is effectively single-use. Once the rails and aprons have folded, further hits go straight into the cage and suspension mounts, and the structure loses integrity faster. Diamond or twist on a unibody is effectively "totalled".
- The IIHS speed finding suggests a nonlinear model: below a threshold, energy goes into crush zones (cosmetic and minor damage). Above it, energy spills into "core" systems such as the cabin, engine and suspension mounts.
- Kinetic energy is ½mv² (general physics), so doubling closing speed quadruples the energy to absorb. Heavier full-size derby cars (roughly 4,000+ lb; this weight figure is not verified here) carry more energy at the same speed.

### Gaps
- No engineering-grade source was found that directly compares the crush behaviour of a 1970s full-frame sedan with a modern unibody under repeated impacts. This is a research gap, not just a session limit.
- No source was found on how full-frame rails buckle in derby impacts specifically.

## 2. Failure order by impact type (front, rear, side/T-bone, rollover, repeated)

### Takeaway
- **Front:** load goes bumper/crash box, then radiator support and radiator, then frame rails and engine, then firewall and toe-pan. An offset corner hit instead loads the wheel, suspension and hinge pillar, shoving the wheel back.
- **Rear:** the trunk and rear rails crush, and the axle or differential can be shoved forward, pushing the driveshaft into the transmission.
- **Side:** the door is the weak point, and the B-pillar and sill limit intrusion.
- **Rollover:** load goes into the A-pillars and roof, and the windshield carries a big share of roof strength.

### Cited Findings
**Frontal**
- Frontal load path: bumper and crash box, then front longitudinal rails, then engine, then firewall. In one model, force peaked in the rails at about 15.5–16 ms. This is a student exercise; treat the numbers as illustrative — [Skill-Lync BIW frontal crash](https://skill-lync.com/student-projects/assignment-6-139)
- High-fidelity full-frontal simulation: crash boxes and rails fold progressively, with snapshots at 15, 50 and 75 ms. The dominant intrusion comes from front rail deformation, firewall and toe-pan movement — [Nature Scientific Reports, 2026](https://www.nature.com/articles/s41598-026-43474-z)
- In a frame simulation, most impact energy was absorbed by the front rails and the engine within about 0.035 s — [ScienceDirect, FE analysis of car frame frontal crash](https://www.sciencedirect.com/science/article/pii/S230718772300007X)
- The whole frontal event lasts roughly 70–120 ms: about 70–90 ms per [Firgelli calculator](https://www.firgelliauto.com/blogs/engineering-calculators/car-crash-force-calculator) and about 120 ms per a chassis-frame study ([ResearchGate](https://www.researchgate.net/publication/331098166_FRONTAL_CRASH_SIMULATION_OF_A_CHASSIS_FRAME)). Methods differ, so these are not one timeline.
- A real NHTSA crash investigation documented deformation of the bumper beam, both frame rails and the lower radiator support in one frontal event — [NHTSA report 813312](https://crashstats.nhtsa.dot.gov/Api/Public/Publication/813312)

**Small-overlap (corner) frontal**
- The IIHS small-overlap test uses a rigid barrier, 25% overlap, 64 km/h (40 mph). Primary loading goes to the wheel, the suspension and the hinge pillar — [IIHS bibliography 2049](https://www.iihs.org/topics/bibliography/ref/2049); [IIHS protocol](https://www.IIHS.org/media/ec54a7ea-1a1d-4fb2-8fc3-b2e018db2082/1A5oYw/Ratings/Protocols/current/small_overlap_test_protocol.pdf)
- It is "not uncommon for the wheel to be forced rearward into the footwell" in small-overlap crashes — [Edmunds](https://www.edmunds.com/car-safety/new-crash-test-shakes-up-car-safety-ratings.html) (search excerpt)
- Observed outcomes:
  - A door hinge pillar tore off completely (Nissan Rogue, passenger side), and a door opened (Toyota RAV4) — [IIHS, Small overlap gap](https://www.iihs.org/news/detail/small-overlap-gap-vehicles-with-good-driver-protection-may-leave-passengers-at-risk)
  - Ford Explorer intrusion of 15 in at the lower hinge pillar and 13 in at the upper hinge pillar and dashboard — [TheTruthAboutCars](https://TheTruthAboutCars.com/2018/06/two-long-tooth-midsize-suvs-fail-latest-round-crash-tests/)
  - The VW CC was the first car to lose a door in an IIHS test — [Claims Journal](https://www.claimsjournal.com/news/national/2012/08/14/211905.htm) (search excerpt)

**Side / T-bone**
- The IIHS side barrier is a deformable honeycomb face that wraps around the B-pillar. The structural rating is driven mainly by B-pillar intrusion: Good means at least 12.5 cm from the driver-seat centerline, and Poor means past the centerline. Doors tend to be weaker than the B-pillar and the surrounding frame. In a 2004 Camry test, the most-intruded B-pillar point was 41.6 cm outboard of centerline — [IIHS side test](https://iihs.org/ratings/about-our-tests/side); [IIHS side impact guide](https://iihs-prod.iihs.org/media/2104caa9-7f7e-41fa-a4a5-af65f1cab89e/l9AWAw/Ratings/Protocols/current/side_impact_guide.pdf)
- In typical side impacts the striking vehicle partly overrides the sill, so load transfers into the door and B-pillar — [FMVSS 214 research via search excerpt](https://index.mirasmart.com/27esv/PDFfiles/27ESV-000036.pdf)
- After the 2022 tougher IIHS side test, 4 of 11 small cars were rated Poor — [IIHS news](https://www.iihs.org/news/detail/small-car-performance-is-a-mixed-bag-in-new-side-crash-test)

**Rear**
- A rear hit can shove the differential forward. One 2015 Silverado owner reported about 4 in, which drove the driveshaft deeper into the transmission so it jammed. A hard rear hit can also slam the torque converter back into the front pump — [JustAnswer, Silverado rear-ended](https://www.justanswer.com/chevy/rcp3s-2015-silverado-2wd-rear-ended-car.html); [Quora, rear-end transmission damage](https://www.quora.com/Can-a-rear-end-collision-damage-a-transmission) (anecdotal/forum)
- Derby builders sometimes create weak spots in the rear so it folds *upward* when hit, which keeps the rear (drive) wheels on the ground — [MotoIQ, Demolition Derby Tech Part 2](https://motoiq.com/demolition-derby-tech-part-2/)
- Some rulebooks allow frame notches so the rear "roll[s]/pack[s]". Others ban notching or pre-bending, or ban mashing the rear down on a fresh car — [USA RWD Derby Rules](https://usademolitionderby.com/usa-rwd-derby-rules/); [Summit County Fair rules](http://www.summitcountyfair.org/demolition-derby-rules-and-regulations-2015)

**Rollover**
- FMVSS 216 roof crush is a quasi-static plate test requiring a strength-to-weight ratio of 3.0 or more (upgraded under the 216a rule). It mainly tests the A-pillar and the roof over the front occupants. More than 30% of a roof's static strength can come from the bonded windshield — [UVA dissertation](https://libraetd.lib.virginia.edu/public_view/mk61rh152); [NHTSA final rule](https://www.nhtsa.gov/document/final-rule-federal-motor-vehicle-safety-standards-roof-crush-resistance-phase-reporting-1); [ESV paper](https://www-nrd.nhtsa.dot.gov/pdf/ESV/esv17/Proceed/00006.pdf)
- Each 1-unit increase in SWR (within 5 in of crush) was associated with a 24–28% lower risk of fatal or incapacitating injury in real rollovers — [IIHS bibliography 1817](https://www.iihs.org/research-areas/bibliography/ref/1817)

### Inferences
- A plausible frontal failure order for a game (synthesised from the load-path sources above):
  1. Bumper and grille
  2. Headlights and radiator support; radiator punctured, with steam
  3. Hood buckles and pops up at the latch
  4. Fenders crush into the tyres
  5. Frame rails accordion (mash) and the engine is pushed back; fan into radiator, engine mounts
  6. Firewall and toe-pan intrude; steering column and pedals
- In derby cars, the stock windshield and glass are usually already removed (see section 5), so the rollover roof-strength contribution from glass does not apply. A derby car's roof would crush more easily than a road car's, though most derby rules add a cage or bar near the driver (rule details not verified here).
- Offset corner hits are the most dangerous for mobility, because they load the wheel and suspension directly (wheel pushed back, tie rods and control arms bent). This suggests separate per-corner suspension health in a game.
- For repeated hits: once a crush zone has folded, it no longer absorbs energy, so the next hit at the same spot transmits more force inward. Damage should escalate per location.

### Gaps
- No published order-of-failure study for repeated, multi-direction, low-speed (20–45 mph) impacts like a derby. The sequence above is synthesised.
- No primary data on what happens to an older full-frame car's body mounts (rubber body-to-frame bushings) in hard hits; the body may shift on the frame. Not verified.

## 3. Which failures actually disable a car (out when it stops moving)

### Takeaway
In derbies the main killers are:
- Cooling loss (radiator punctured, then overheat, then stall or seize)
- Steering and front-suspension damage (bent or broken tie rods, bent wheels, tyres jammed against crushed fenders)
- Drivetrain separation (driveshaft pulled out of the transmission, axle shoved forward or jammed, broken axle)
- Fire, which officials stop the car for

The radiator is called the single most vulnerable item.

### Cited Findings
- "The radiator is probably the most vulnerable item in a demolition derby car" — [Speedway Motors, Demolition Derby Radiator Guide](https://www.speedwaymotors.com/the-toolbox/demolition-derby-radiator-guide/29894) (search excerpt; page fetch blocked)
- A front hit "could kill the engine" — [Pomona Swap Meet, Demolition Derby Cars: A History of Chaos](https://pomonaswapmeet.com/blog/2016/01/21/demolition-derby-cars-a-history-of-chaos)
- Derby-parts vendors sell heavy rad guards to shield the radiator from "direct hits, flying debris, and front-end collapse". Some rulebooks ban reinforcing the radiator support — [DerbyParts BMS Radiator Protector](https://derbyparts.com/product/bms-radiator-protector/); [5 Flags Speedway rules](https://www.5flagsspeedway.com/downloads/get.aspx?i=739576)
- Derby-parts guidance lists weak links that end runs: bent tie rods "that make the engine labor and scrub power", and bent wheels that "shake, drag, and slow the car" — [DerbyParts, How to Make It to the Feature](https://derbyparts.com/how-to-make-it-to-the-feature/) (search excerpt)
- Tie rods are a recognised failure point. Rulebooks variously allow reinforcement (for example with 1/2" rebar or aftermarket parts) or ban it. A combine-derby rule allows repairing a broken tie rod or axle between heats — [Summit County Fair](http://www.summitcountyfair.org/demolition-derby-rules-and-regulations-2015); [Jackson County Jr Fair](https://www.jacksoncountyjrfair.org/derby-rules); [MAP Motorsports](https://mapmotorsports.com/rules-2)
- MotoIQ's own derby car was finally stopped when "the driveshaft [fell] out of the transmission" — [MotoIQ, Demolition Derby Tech Part 2](https://motoiq.com/demolition-derby-tech-part-2/)
- One derby anecdote: a car that "if [it] wasn't hot enough … sputtered and stalled", so engine temperature affects running both ways — [search result referencing derby press coverage](https://www.lmtribune.com/northwest/demolition-derby-is-smashing-good-fun-7130a3b3) (weak source)
- How long an engine runs with no coolant: estimates range from about 90 seconds (enthusiast anecdote) to "a few minutes or an hour depending how hard you ran it". Hot weather shortens the window. The end results are a warped head, a blown head gasket and a seized engine. There is no reliable test data — [science.ca Ask a Scientist](https://www.science.ca/askascientist/viewquestion.php?qID=1532); [MTFCA forum](https://www.mtfca.com/phpBB3/viewtopic.php?p=131299); [StartRescue](https://startrescue.co.uk/breakdown-cover/motoring-advice/car-servicing-and-repairs/how-far-can-you-drive-an-overheating-car)
- Rulebooks require: fuel systems leak-free; engines degreased and free of flammables; a fire extinguisher within the driver's reach; hood holes (at least 4 in) for extinguisher access to both sides of the engine. This shows engine-bay fires are an expected event — [Burn Institute Firefighter Derby Rules 2025](https://burninstitute.org/wp-content/uploads/2025/02/2025-Firefighter-Demolition-Derby-Rules.pdf); [Calhoun County rules](https://calhouncountyfairgrounds.com/wp-content/uploads/2025_Demolition-Derby_Rules.pdf); [5 Flags Speedway rules](https://www.5flagsspeedway.com/downloads/get.aspx?i=739576)
- Stock fuel tanks must be removed and replaced with a small tank or fuel cell (for example a 5-gal limit, steel marine tank or racing cell). It is mounted in the back-seat area, no further forward than the rear of the front seat, or within 18 in forward of the rear axle. Batteries are often moved to the passenger floor and covered — [Alaska State Fair rules](https://www.alaskastatefair.org/site/wp-content/uploads/2025/06/2025-Alaska-State-Fair-Demolition-Derby-Rules-Small-Compact-Cars.pdf); [Yamhill County Import rules](https://www.yamhillcounty.gov/DocumentCenter/View/325/2023-Import-Class-Rules-PDF); [AV Fair Compact rules](https://avfair.com/wp-content/uploads/2025/01/2025-Compact-DD-Rules.pdf)
- Working brakes are required at inspection — [Summit County Fair](http://www.summitcountyfair.org/demolition-derby-rules-and-regulations-2015)

### Inferences
- A failure-state list for a game, mapped to real causes:
  1. **Cooling**
     - Radiator punctured → coolant loss → temperature climbs → power loss → stall or seize after a delay (tens of seconds to minutes).
     - The delay is a good gameplay window: steam shows a "dying" car that can still make a last hit.
  2. **Steering / front suspension**
     - Tie rod bent: the car pulls, the wheels toe in or out, there is drag and lost speed.
     - Tie rod broken: the wheel flops and the car has no steering on that side.
     - Wheel "tucked": the wheel is pushed back or under, there is no steering, and the car is often immobile.
  3. **Wheels and tyres**
     - Bent rim → shaking and drag.
     - Tyre jammed against a crushed fender or quarter panel → wheel locked, so a drive wheel scrubs or stops.
     - Tyre off the rim → riding on steel, slow and sparking.
  4. **Drivetrain**
     - Driveshaft out of the transmission, or a broken axle → the engine revs but the car does not move. A classic "out" state.
     - Differential or axle shoved forward → jammed or bound.
  5. **Engine:** stalled from overheating, or a dislodged fan, belts or alternator (battery drains). Not sourced specifically; plausible.
  6. **Fire:** fuel or oil on a hot engine, or electrical. Officials typically stop the car. Fuel-tank relocation makes tank fires rare in derbies.
- Rear-wheel-drive derby cars (the classic full-size sedans) are disabled mainly by rear-axle or driveshaft damage or by both rear wheels losing traction. Front damage disables them through cooling and steering. A game can make the *drive* end and the *engine* end each a vulnerability with different consequences.

### Gaps
- No quantitative data was found on how often each cause eliminates cars (overheat vs steering vs drivetrain).
- No reliable time-to-seize figure with zero coolant; only anecdotes.
- No data on derby fire frequency.

## 4. Why derby drivers hit with the rear and protect the front

### Takeaway
The engine, radiator, fan and steering linkage all sit in the front of a front-engine, rear-drive full-size car. Hitting with the trunk spends "disposable" structure — the trunk, rear rails and bumper are ahead of the rear axle — instead of the cooling system and steering that keep the car alive. Builders even tune the rear to fold upward so the drive wheels stay planted.

### Cited Findings
- Survey of 40 derby drivers (2005): 52 collisions per event on average, 55% of them rear-end. Mean collision speed 26 mph, maximum 45 mph — [Neck pain in demolition derby drivers, Weill Cornell (VIVO record)](https://vivo.weill.cornell.edu/display/pubid15827919); summarised in [The Society Pages](https://thesocietypages.org/editors/2012/12/10/pie-demolition-derby-and-the-social-construction-of-injury/)
- "Derbiers still hit with the rear ends, but it's not like it used to be", meaning front-end contact is now more common, alongside the radiator being the most vulnerable item — [Speedway Motors radiator guide](https://www.speedwaymotors.com/the-toolbox/demolition-derby-radiator-guide/29894) (excerpt)
- Anecdote: a rookie who led with his radiator was knocked out by the head-on hit and said, "Everybody says to hit from the back" — search result for derby press coverage ([Fairborn Daily Herald](https://www.fairborndailyherald.com/?p=19165) / [Farm and Dairy](https://farmanddairy.com/?p=7868); exact attribution unclear from the excerpt)
- Builders weaken the rear on purpose so it folds upward and the rear wheel isn't lifted off the ground — [MotoIQ](https://motoiq.com/demolition-derby-tech-part-2/)
- The heaviest hits are often two cars meeting in reverse at full speed, with closing speeds over 50 mph — [MotoIQ](https://motoiq.com/demolition-derby-tech-part-2/)

### Inferences
- Gameplay implication: reverse-ramming should be the "skilled, efficient" attack. The rear has a big crushable buffer with low system criticality. The cost is that repeated rear hits eventually reach the rear axle, wheels and driveshaft, which do disable an RWD car. Rear tyres rubbing on crushed quarter panels is the classic late-match rear failure, and rulebooks allow trimming fenders for tyre clearance, which shows how common it is — [Summit County Fair](http://www.summitcountyfair.org/demolition-derby-rules-and-regulations-2015).
- Steering is reversed and visibility poor when driving backwards. This gives a natural skill trade-off for a game.
- Hitting an opponent's front wheel or the side of its front end ("go for the radiator or the front wheel") is the real offensive target, because it maps to the main disabling failures in section 3.

### Gaps
- No primary statistics on what share of eliminations come from front damage vs rear damage.

## 5. Visual and audio cues of damage

### Takeaway
Derby cars are stripped of glass and trim before the event, and hoods are holed or chained. The visible damage vocabulary is mostly: crumpled sheet metal; rippled roofs and quarters; hoods buckled or popped; trunks folded up; wheels splayed or tucked; tyres off rims; steam from the radiator; smoke and occasional fire from the engine bay; sparks from metal on concrete; dragging bumpers and parts.

### Cited Findings
- Derby rules commonly require removing all glass, including the windshield in some classes. Removing nuts, bolts, glass and loose items also keeps the track clean — [Burn Institute rules](https://burninstitute.org/wp-content/uploads/2025/02/2025-Firefighter-Demolition-Derby-Rules.pdf); [Alaska State Fair rules](https://www.alaskastatefair.org/site/wp-content/uploads/2025/06/2025-Alaska-State-Fair-Demolition-Derby-Rules-Small-Compact-Cars.pdf)
- Hoods and trunks may be folded over and chained or wired. Some tracks require the trunk to stay on its hinges. Hoods need holes for extinguisher access — [Summit County Fair](http://www.summitcountyfair.org/demolition-derby-rules-and-regulations-2015); [5 Flags Speedway](https://www.5flagsspeedway.com/downloads/get.aspx?i=739576)
- Frame-damage tell-tales: uneven panel gaps (sag shows as gaps wide at the bottom and narrow at the top); the car visibly leaning or crabbing (sway, dog-tracking from diamond) — [Cash Auto Salvage](https://www.cashautosalvage.com/frame-damage/); [FenderBender](https://www.fenderbender.com/running-a-shop/operations/article/33019960/advanced-structural-analysis)
- Small-overlap crashes visibly push the front wheel rearward and can tear doors or hinge pillars open — [IIHS](https://www.iihs.org/news/detail/small-overlap-gap-vehicles-with-good-driver-protection-may-leave-passengers-at-risk)
- Rear folded upward deliberately, giving a raised and crumpled trunk — [MotoIQ](https://motoiq.com/demolition-derby-tech-part-2/)
- Spinning wheels reach "far higher" speeds than the car; cars reach about 30–40 mph — [Lewiston Tribune](https://www.lmtribune.com/northwest/demolition-derby-is-smashing-good-fun-7130a3b3)

### Inferences (general knowledge, not separately sourced in this session)
- **Steam (white):** radiator or hose rupture. Effect spawns at the grille or hood gaps and grows as coolant drains, then stops when the coolant is gone, shortly before overheating.
- **Smoke:**
  - Blue-grey: oil burning on the exhaust, or a blown head gasket.
  - Black: rich running or a fire.
  - White-grey from the wheel wells: tyres rubbing on crushed fenders.
- **Sparks:** steel rims on concrete after a tyre comes off; dragging bumpers and exhausts; frame scraping when the suspension collapses.
- **Popped hood:** the latch fails when the radiator support and fenders crush back, and the hood buckles upward in the middle (a classic accordion "tent").
- **Wheel cues:** the camber changes (top of the tyre tilts in or out) after control-arm or tie-rod damage; the wheel is tucked back into the wheel well; the tyre is flat or off the bead.
- **Stance:** the car sits nose-down or crooked after a spring or frame sag; the trunk is raised (folded rear).
- **Audio:** fan blade striking the radiator or shroud (rhythmic clatter); belt squeal; dragging-metal scrape; engine revving with no movement (driveshaft gone).

### Gaps
- No authoritative source catalogues derby visual cues. Most of this is general automotive knowledge and should be checked against derby video footage.

## 6. Quantitative data (crush vs speed, energy, derby speeds)

### Takeaway
Derby collisions are moderate-speed (mean about 26 mph, maximum about 45 mph; reverse-to-reverse closing speeds over 50 mph) but very frequent (about 52 per event). Classic crush-energy models treat residual crush as roughly linear with barrier-equivalent impact speed, which is a simple, defensible basis for a game's damage model.

### Cited Findings
- Derby driver survey (n=40): mean collision speed 26 mph, maximum 45 mph, about 52 collisions per event, 55% rear-end — [Weill Cornell VIVO](https://vivo.weill.cornell.edu/display/pubid15827919)
- A press estimate puts derby car speeds at 30–40 mph — [Lewiston Tribune](https://www.lmtribune.com/northwest/demolition-derby-is-smashing-good-fun-7130a3b3); closing speeds over 50 mph in reverse-to-reverse hits — [MotoIQ](https://motoiq.com/demolition-derby-tech-part-2/)
- Campbell (SAE 740565, 1974), "Energy Basis for Collision Severity": frontal barrier tests showed a roughly *linear* relationship between impact speed and residual crush. The model has the form V = b0 + b1·C, where V is the equivalent barrier speed and C is the average crush. The CRASH program (McHenry) recast this as a crush-force model with stiffness coefficients A (force per unit width at onset of crush) and B (stiffness per unit crush) — [IRJET summary](https://issuu.com/irjet/docs/irjet-v10i391); [NAFE journal article](https://journal.nafe.org/ojs/index.php/nafe/article/download/519/489/1494); [J.S. Held, Vehicle Frontal Crush Stiffness Coefficients Trends](https://jsheld.com/uploads/Vehicle-Frontal-Crush-Stiffness-Coefficients-Trends.pdf)
- b0 represents the speed below which there is no permanent crush, an elastic "no-damage" threshold. In the CRASH formulation, the energy absorbed per unit width is A·C + B·C²/2 + A²/(2B), so energy grows with the *square* of crush. This is from the standard CRASH3 formulation as described in the sources above; exact coefficient values could not be retrieved because the PDFs were blocked.
- Campbell- and CRASH-type coefficients were derived from 1970s–90s structures and reportedly under-predict impact speed for modern vehicles, which are stiffer — [engrXiv preprint](https://engrxiv.org/preprint/download/7697/12575/10872) (abstract only)
- Ignoring restitution (rebound) when deriving or applying crush coefficients tends to underestimate speed change — [SAE 2010-01-0069](https://saemobilus.sae.org/content/2010-01-0069) (abstract)
- Frontal crash pulse duration is about 70–120 ms — [Firgelli](https://www.firgelliauto.com/blogs/engineering-calculators/car-crash-force-calculator); [ResearchGate](https://www.researchgate.net/publication/331098166_FRONTAL_CRASH_SIMULATION_OF_A_CHASSIS_FRAME)
- Standard test speeds: IIHS small overlap at 64 km/h (40 mph) — [IIHS](https://www.iihs.org/topics/bibliography/ref/2049). FMVSS 216 roof: SWR of 3.0 or more within 5 in (127 mm) of plate travel. One theoretical analysis says about 3.6 is needed — [ACRS archive](https://archive.acrs.org.au/article/vehicle-roof-strength-as-it-relates-to-contained-occupant-injury-prevention-during-rollover-crashes)
- Reported intrusion magnitudes: up to 15 in at the lower hinge pillar (Explorer, small overlap) — [TheTruthAboutCars](https://TheTruthAboutCars.com/2018/06/two-long-tooth-midsize-suvs-fail-latest-round-crash-tests/); about 4 in of differential set-back from a rear hit (anecdotal) — [JustAnswer](https://www.justanswer.com/chevy/rcp3s-2015-silverado-2wd-rear-ended-car.html)

### Inferences
- Simple game damage model grounded in Campbell/CRASH:
  - **Permanent dent depth:** crush ≈ max(0, (v_closing_component − v0) / k), with v0 ≈ a few mph elastic threshold (b0) and k set per zone (stiff frame horn vs soft trunk).
  - **Energy absorbed:** grows with crush² plus a linear term. Distribute ½·μ·Δv² (reduced mass μ) across the contact zones by local stiffness.
  - **Repeated hits:** pre-crushed metal has already "used up" its A term, so it deforms more for the same energy until it bottoms out on a hard component (engine block, axle, frame), where stiffness jumps sharply.
- At derby speeds of 20–45 mph, a single hit can plausibly produce crush of the order of inches to about a foot in soft zones. Real classic-car crush at a 30 mph barrier could not be confirmed here; treat as a tuning value.
- With about 52 collisions per event, damage per hit should be modest and cumulative. Disabling should come mainly from critical subsystems (radiator, tie rods, wheels, driveshaft) rather than total structural crush.

### Gaps
- No actual A/B or b0/b1 coefficient values for 1970s–90s full-size sedans were retrieved, because the J.S. Held and NAFE PDFs were blocked. Fetching the J.S. Held PDF would give typical values by model year and vehicle class.
- No published crush-depth-vs-speed table for a classic body-on-frame sedan was retrieved.
- Hosts that need to be allowed for deeper sourcing: speedwaymotors.com, derbyparts.com, journal.nafe.org, thedailyautomotive.com.
