(ns orc_battle_online.html_rendering
  (:use (orc_battle_online game_logic stateful_links))
  (:use (ring.util response))
  (:use (hiccup core page-helpers form-helpers)))

(defn response-html [body]
  (-> (response body)
      (content-type "text/html")))

(defn monster-show-html [m]
  (str (with-out-str (monster-show m)) " (Health: " (:health m) ")"))

(defn show-monsters-html []
  (html (ordered-list (map (fn [m]
	 (if (monster-dead m)
	   "*dead*"
	   (monster-show-html m)))
       @*monsters*))))

(defn show-actions-html []
  (html (create-link "Stab"
		     (fn [req]
		       (response-html
			(html
			 (ordered-list
			  (map
			   #(str (monster-show-html %1)
				 (create-link "Stab this monster"
					      (fn [req]
						(let [in-str (str "s\r\n" %2 "\r\n")]
						  (println "in-str =" in-str)
						  (with-in-str in-str (player-attack))
						  (-> (redirect "/main")
						      (assoc :flash (str "You stabbed monster " %2)))))))
				 @*monsters*
				 (iterate inc 1)))))))
	[:br]
	[:a {:href "roundhouse"} "Roundhouse"]
	[:br]
	[:a {:href "choose-double-swing-target"} "Double swing"]))

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
