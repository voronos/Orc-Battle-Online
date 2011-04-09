(ns orc_battle_online.html_rendering
  (:use (orc_battle_online game_logic stateful_links))
  (:use (ring.util response))
  (:use (hiccup core page-helpers form-helpers)))

(defn response-html [body]
  (-> (response body)
      (content-type "text/html")))

(defn monster-show-html [m]
  (str (with-out-str (monster-show m)) " (Health: " (:health m) ")"))

(defn monster-show-with-attack [m fun]
  (if (monster-dead m)
    "*dead*"
    (create-link (monster-show-html m) fun)))

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
			      #(monster-show-with-attack %1 
				 (fn [req]
				   (let [in-str (str "s\r\n" %2 "\r\n")]
				     (println "in-str =" in-str)
				     (with-in-str in-str (player-attack))
				     (-> (redirect "/main")
					 (assoc :flash (str "You stabbed monster " %2)))))))))))))

(def roundhouse-link
     (create-link "Roundhouse"
		  (fn [req]
		    (let [output (with-out-str (with-in-str "r\r\n" (player-attack)))]
		      (-> (redirect "/main")
			  (assoc :flash output))))))

(defn second-double-swing-choice [first-i m i]
  (monster-show-with-attack m
    (fn [req]
      (let [in-str (str "d\r\n" first-i "\r\n" i)]
	(with-in-str in-str (player-attack))
	(-> (redirect "/main")
	    (assoc :flash in-str))))))

(defn first-double-swing-choice [m i]
  (monster-show-with-attack m
    (fn [req]
      (response-html
       (html [:p "Which monster will you hit second?"]
	     (ordered-list (map-monsters-with-index (partial second-double-swing-choice i))))))))

(def double-swing-link
     (create-link "Double swing"
		  (fn [req]
		    (response-html
		     (html [:p "Which monster will you hit first?"]
			   (ordered-list
			    (map-monsters-with-index first-double-swing-choice)))))))

(defn show-actions-html []
  (html stab-link
	[:br]
	roundhouse-link
	[:br]
	double-swing-link
	[:br]
	[:a {:href "/newgame"} "New Game"]))

(defn show-player-html []
  (html [:p
	 (str "You are a valiant knight with a health of " @*player-health*
	      ", an agility of " @*player-agility*
	      ", and a strength of " @*player-strength*)]))

(defn render-game-html [req]
  (html4 [:body
	  (if (:flash req) [:div#flash [:pre (:flash req)]])
	  ;(if (get-in req [:session :_flash]) [:div#_flash (get-in req [:session :_flash])])
	  [:div#player (show-player-html)]
	  [:div#monsters (show-monsters-html)]
	  [:div#actions (show-actions-html)]]))
