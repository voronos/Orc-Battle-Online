(ns orc_battle_online.html_rendering
  (:use (orc_battle_online game_logic stateful_links))
  (:use (ring.util response))
  (:use (hiccup core page-helpers form-helpers)))

(defn response-html [body]
  (-> (response body)
      (content-type "text/html")))

(defn monster-show-html [m]
  (str (with-out-str (monster-show m)) " (Health: " (:health m) ")"))

(defn monster-show-with-attack [m attack-fun]
  (if (monster-dead m)
    "*dead*"
    (create-link (monster-show-html m) attack-fun)))

(defn show-monsters-html []
  (html (ordered-list (map (fn [m]
			     (if (monster-dead m)
			       "*dead*"
			       (monster-show-html m)))
			   @*monsters*))))

(defn map-monsters-with-index [fun]
  (map fun @*monsters* (iterate inc 1)))

(def stab-link
  (create-link "Stab"
               (fn [req]
                 (response-html
                  (html [:p "Which monster will you stab?"]
                        (ordered-list
                         (map-monsters-with-index
                           (fn [m i] (monster-show-with-attack m
                                       (fn [req]
                                         (stab-monster [(dec i) m])
                                         (-> (redirect "/main")
                                             (assoc :flash (str "You stabbed monster " i)))))))))))))

(def roundhouse-link
     (create-link "Roundhouse"
		  (fn [req]
		    (let [output (with-out-str (roundhouse-attack))]
		      (-> (redirect "/main")
			  (assoc :flash output))))))

(defn second-double-swing-choice [attack-fun m i]
  (monster-show-with-attack m
    (fn [req]
      (attack-fun [(dec i) m])
      (-> (redirect "/main")
          (assoc :flash (str "You hit monster " i))))))

(defn first-double-swing-choice [attack-fun m i]
  (monster-show-with-attack m
    (fn [req]
      (attack-fun [(dec i) m])
      (if (monsters-dead)
        (-> (redirect "/main") (assoc :flash "No monsters left alive"))
        (response-html
         (html [:p "You hit monster " i ". Which monster will you hit second?"]
               (ordered-list (map-monsters-with-index (partial second-double-swing-choice attack-fun)))))))))

(def double-swing-link
  (create-link "Double swing"
               (fn [req]
                 (let [[attack-strength attack-fun] (double-swing-attack)]
                   (response-html
                    (html [:p "Your double swing has a strength of "
                           attack-strength ". Which monster will you hit first?"]
                          (ordered-list
                           (map-monsters-with-index (partial first-double-swing-choice attack-fun)))))))))

(defn show-actions-html []
  (html stab-link
	[:br]
	roundhouse-link
	[:br]
	double-swing-link
	[:br]
	[:a {:href "/newgame"} "New Game"]))

(defn show-player-html [turn-number]
  (html [:p (str "Turn " turn-number)]
        [:p
	 (str "You are a valiant knight with a health of " @*player-health*
	      ", an agility of " @*player-agility*
	      ", and a strength of " @*player-strength*)]))

(defn render-game-html [req turn-number]
  (html4 [:body
	  (if (:flash req) [:div#flash [:pre (:flash req)]])
	  [:div#player (show-player-html turn-number)]
	  [:div#monsters (show-monsters-html)]
	  [:div#actions (show-actions-html)]]))
