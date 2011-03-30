(ns orc_battle_online.game_logic)

(defmulti monster-hit (fn [i-m-pair amount] (:type (fnext i-m-pair))))
(defmulti monster-show :type)
(defmulti monster-attack :type)

(defn randval [n]
  (+ 1 (rand-int (max 1 n))))

(defstruct monster :health :type)
(defn make-monster [type] (struct monster (randval 10) type))
;(defn make-monster []
;  (struct monster (randval 10) nil))
(defn make-orc []
  (assoc (make-monster 'orc) :club-level (randval 8)))
(defn make-hydra []
  (make-monster 'hydra))
(defn make-slime-mold []
  (assoc (make-monster 'slime-mold) :sliminess (randval 5)))

(defn make-brigand []
  (make-monster 'brigand))

(def *player-health* (atom nil))
(def *player-agility* (atom nil))
(def *player-strength* (atom nil))
(def *monsters* (atom []))
(def *monster-builders* (atom (list make-orc make-hydra make-slime-mold make-brigand)))
(def *monster-num* 12)

(defn init-monsters []
  (swap! *monsters* (fn [_]
		      (vec (map (fn [x]
			     (apply (nth @*monster-builders* (rand-int (count @*monster-builders*))) []))
			   (repeat *monster-num* 1))))))
				    

(defn init-player []
  (swap! *player-health* (constantly 30))
  (swap! *player-agility* (constantly 30))
  (swap! *player-strength* (constantly 30)))

(defn player-dead []
  (<= @*player-health* 0))

(defn monster-dead [m]
  (<= (:health m) 0))

(defn monsters-dead []
  (every? monster-dead @*monsters*))

(defn game-read [request-prompt request-exit]
  )

(defn show-player []
  (println)
  (print "You are a valiant knight with a health of" @*player-health*)
  (print ", an agility of" @*player-agility*)
  (print ", and a strength of" @*player-strength*))

(defn show-monsters []
  (println)
  (print "Your foes:")
  (doseq [[i m] (map list (range 1 (inc (count @*monsters*))) @*monsters*)]
    (println)
    (print "  " i)
    (print ". ")
    (if (monster-dead m)
      (print "**dead**")
      (do (print "(Health=")
	  (print (:health m))
	  (print") ")
	  (monster-show m)))))

(defn random-monster []
  (let [i (int (rand-int (count @*monsters*)))
	m (get @*monsters* i)]
    (if (monster-dead m)
      (random-monster)
      [i m])))

(def pick-monster)
(defn pick-monster-result [x]
  (if (not (and (integer? x) (>= x 1) (<= x *monster-num*)))
    (do (print "That is not a valid monster number.") (pick-monster))
    (let [m (get @*monsters* (- x 1))]
      (if (monster-dead m)
	(do (print "That monster is already dead.") (pick-monster))
	[(- x 1) m]))))

(defn pick-monster []
  (println)
  (print "Monster #:")
  (flush)
  (let [x (read)]
    (println)
    (pick-monster-result x)))

(defmethod monster-hit :default [m x]
	   (let [new-hp (- (:health (fnext m)) x)]
	     (swap! *monsters* assoc-in [(first m) :health] new-hp))
	   (let [new-m (get @*monsters* (first m))]
	     (if (monster-dead new-m)
	       (print "You killed the" (:type new-m) "! ")
	       (print "You hit the" (:type new-m) ", knocking off" x "health points!"))))

(defmethod monster-hit 'hydra [m x]
	   (let [new-hp (- (:health (fnext m)) x)]
	     (swap! *monsters* assoc-in [(first m) :health] new-hp))
	   (let [new-m (get @*monsters* (first m))]
	     (if (monster-dead new-m)
	       (print "The corpse of the fully decapitated and decapicated hydra falls to the floor!")
	       (print "You lop off" x "of the hydra's heads! "))))

(defmethod monster-show :default [m] (print "A fierce" (:type m)) m)
(defmethod monster-show 'orc [m] (print "A wicked orc with a level" (:club-level m) "club.") m)
(defmethod monster-show 'hydra [m]
	   (print "A malicious hydra with" (:health m) "heads.") m)
(defmethod monster-show 'slime-mold [m]
	   (print "A slime mold with a sliminess of" (:sliminess m)))

(defmethod monster-attack :default [m])
(defmethod monster-attack 'orc [m]
	   (let [x (randval (:club-level m))]
	     (print "An orc swings his club at you and knocks off" x)
	     (print " of your health points. ")
	     (swap! *player-health* - x)))

(defmethod monster-attack 'hydra [m]
	   (let [x (randval (bit-shift-left (:health m) -1))]
	     (print "A hydra attacks you with" x "of its heads! It also grows back one more head!")
	     (swap! *monsters* (fn [m-lst new-m]
				 (assoc m-lst
				   (some #(if (= (get m-lst %) m) %) (range (count m-lst))) new-m))
		    (update-in m [:health] inc))
	     (swap! *player-health* - x)))

(defmethod monster-attack 'slime-mold [m]
	   (let [x (randval (:sliminess m))]
	     (print "A slime mold wraps around your legs and decreases your agility by" x)
	     (print "! ")
	     (swap! *player-agility* - x)
	     (when (zero? (rand-int 2))
	       (print "It also squirts in your face, taking away a health point! ")
	       (swap! *player-health* dec))))

(defmethod monster-attack 'brigand [m]
	   (let [x (max @*player-health* @*player-agility* @*player-strength*)]
	     (cond
	      (= x @*player-health*) (do (print "A brigand hits you with his slingshot, taking off 2 health points! ") (swap! *player-health* - 2))
	      (= x @*player-agility*) (do (print "A brigand catches your leg with his whip, taking off 2 agility points! ") (swap! *player-agility* - 2))
	      (= x @*player-strength*) (do (print "A brigand cuts your arm with his whip, taking off 2 strength points! ") (swap! *player-strength* - 2)))))

(defn player-attack []
  (do
    (println)
    (print "Attack style: [s]tab [d]ouble swing [r]oundhouse:")
    (flush))
  (let [attack (read)]
    (do
      (cond
       (= 's attack) (monster-hit (pick-monster) (+ 2 (randval (bit-shift-left @*player-strength* -1))))
       (= 'd attack) (let [x (randval (int (/ @*player-strength* 6)))]
		       (println "Your double swing has a strength of" x)
		       (monster-hit (pick-monster) x)
		       (if-not (monsters-dead)
			 (monster-hit (pick-monster) x)))
       true (dotimes [x (+ 1 (randval (int (/ @*player-strength* 3))))]
	      (if-not (monsters-dead)
		(monster-hit (random-monster) 1))))))
  (println))

(defn game-loop []
  (when-not (or (player-dead) (monsters-dead))
    (show-player)
    (doseq [i (range (+ 1 (int (/ (max 0 @*player-agility*) 15))))]
      (when-not (monsters-dead)
	(show-monsters)
	(player-attack)))
    (println)
    (flush)
    (doseq [m @*monsters*]
      (or (monster-dead m) (monster-attack m)))
    (recur)))

(defn orc-battle []
  (init-monsters)
  (init-player)
  (game-loop)
  (when (player-dead)
    (println "You have been killed. Game Over."))
  (when (monsters-dead)
    (println "Congratulations! You have vanquished all of your foes.")))