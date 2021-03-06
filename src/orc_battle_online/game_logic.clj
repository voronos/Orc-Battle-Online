(ns orc_battle_online.game_logic
  (:require clojure.string))

(defmulti monster-hit (fn [i-m-pair amount] (:type (fnext i-m-pair))))
(defmulti monster-show :type)
(defmulti monster-attack :type)
(def *turn-counter* (atom 1))

(defn randval [n]
  (+ 1 (rand-int (max 1 n))))

(defstruct monster :health :type)
(defn make-monster [type] (struct monster (randval 10) type))
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
  (swap! *monsters*
         (fn [_]
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
    (let [m (get @*monsters* (dec x))]
      (if (monster-dead m)
	(do (print "That monster is already dead.") (pick-monster))
	[(dec x) m]))))

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
      (str "You killed the " (:type new-m) "! ")
      (str "You hit the " (:type new-m) ", knocking off " x " health points!"))))

(defmethod monster-hit 'hydra [m x]
  (let [new-hp (- (:health (fnext m)) x)]
    (swap! *monsters* assoc-in [(first m) :health] new-hp))
  (let [new-m (get @*monsters* (first m))]
    (if (monster-dead new-m)
      (str "The corpse of the fully decapitated and decapicated hydra falls to the floor!")
      (str "You lop off " x " of the hydra's heads! "))))

(defmethod monster-show :default [m] (print "A fierce" (:type m)) m)
(defmethod monster-show 'orc [m] (print "A wicked orc with a level" (:club-level m) "club.") m)
(defmethod monster-show 'hydra [m]
  (print "A malicious hydra with" (:health m) "heads.") m)
(defmethod monster-show 'slime-mold [m]
  (print "A slime mold with a sliminess of" (:sliminess m)))

(defmethod monster-attack :default [m])
(defmethod monster-attack 'orc [m]
  (let [x (randval (:club-level m))]
    (swap! *player-health* - x)
    (str "An orc swings his club at you and knocks off " x
         " of your health points.")))

(defmethod monster-attack 'hydra [m]
  (let [x (randval (bit-shift-left (:health m) -1))]
    (swap! *monsters* (fn [m-lst new-m]
                        (assoc m-lst
                          (some #(if (= (get m-lst %) m) %) (range (count m-lst))) new-m))
           (update-in m [:health] inc))
    (swap! *player-health* - x)
    (str "A hydra attacks you with " x " of its heads! It also grows back one more head!")))

(defmethod monster-attack 'slime-mold [m]
  (let [x (randval (:sliminess m))
        output (str "A slime mold wraps around your legs and decreases your agility by " x "!")]
    (swap! *player-agility* - x)
    (if (zero? (rand-int 2))
      (do
        (swap! *player-health* dec)
        (str output " It also squirts in your face, taking away a health point! "))
      output)))

(defmethod monster-attack 'brigand [m]
  (let [x (max @*player-health* @*player-agility* @*player-strength*)]
    (cond
     (= x @*player-health*) (do (swap! *player-health* - 2) (str "A brigand hits you with his slingshot, taking off 2 health points! "))
     (= x @*player-agility*) (do (swap! *player-agility* - 2) (str "A brigand catches your leg with his whip, taking off 2 agility points! "))
     (= x @*player-strength*) (do (swap! *player-strength* - 2) (str "A brigand cuts your arm with his whip, taking off 2 strength points! ")))))

(defn stab-monster [i-m-pair]
  (monster-hit i-m-pair (+ 2 (randval (bit-shift-left @*player-strength* -1)))))

(defn double-swing-attack []
  (let [attack-strength (randval (int (/ @*player-strength* 6)))]
    [attack-strength
     (fn [i-m-pair] (monster-hit i-m-pair attack-strength))]))

(defn roundhouse-attack []
  (clojure.string/join "\n" (map (fn [x] (if-not (monsters-dead) (monster-hit (random-monster) 1))) (range (+ (randval (int (/ @*player-strength* 3))))))))

(defn player-attack []
  (do
    (println)
    (print "Attack style: [s]tab [d]ouble swing [r]oundhouse:")
    (flush))
  (let [attack (read)]
    (do
      (print (cond
              (= 's attack) (stab-monster (pick-monster))
              (= 'd attack) (let [[x attack-fun] (double-swing-attack)]
                              (println "Your double swing has a strength of" x)
                              (println (attack-fun (pick-monster)))
                              (if-not (monsters-dead) (attack-fun (pick-monster)) ""))
              true (roundhouse-attack)))))
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
      (or (monster-dead m) (println (monster-attack m))))
    (recur)))

; TODO this needs quite a bit of rework to work well with Ring and not just the console
; I wonder if we could devise a method of passing in functions to the player-attack function to handle getting a result?
(defn orc-battle []
  (init-monsters)
  (init-player)
  (game-loop)
  (when (player-dead)
    (println "You have been killed. Game Over."))
  (when (monsters-dead)
    (println "Congratulations! You have vanquished all of your foes.")))

(defn -main [] (orc-battle))