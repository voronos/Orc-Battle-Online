(ns orc_battle_online.core
  (:use ring.adapter.jetty)
  (:use ring.handler.dump)
  (:use (ring.middleware flash reload stacktrace keyword-params params session))
  (:use (ring.middleware.session memory))
  (:use ring.util.response)
  (:use (hiccup core form-helpers))
  (:use (orc_battle_online [game_logic :exclude [-main]]))
  (:use (orc_battle_online html_rendering stateful_links)))

(defmulti handler :uri)

(defn main-handler [req]
  ;; okay, so it looks like using a multimethod is not the best way to handle
  ;; mapping requests with immutant. We need to define the routes in immutant.clj
  ;; Also, using "/newgame" as a link href will go to the base url, rather than
  ;; our current subset of desired operations
  (let [newgame-link [:a {:href "/newgame"} "New Game?"]]
    (if (monsters-dead)
      (response-html (html [:p (if (:flash req) [:div#flash [:p (:flash req)]])
                            "Congratulations! You have defeated all the monsters!"]
                           newgame-link))
      (if (player-dead)
        (response-html (html [:p (if (:flash req) [:div#flash [:p (:flash req)]])
                              "Too bad. You got slaughtered."]
                             newgame-link))
        (response-html (str (render-game-html req @*turn-counter*)))))))

(defmethod handler "/main" [req]
  (let [newgame-link [:a {:href "/newgame"} "New Game?"]]
    (if (monsters-dead)
      (response-html (html [:p (if (:flash req) [:div#flash [:p (:flash req)]])
                            "Congratulations! You have defeated all the monsters!"]
                           newgame-link))
      (if (player-dead)
        (response-html (html [:p (if (:flash req) [:div#flash [:p (:flash req)]])
                              "Too bad. You got slaughtered."]
                             newgame-link))
        (response-html (str (render-game-html req @*turn-counter*)))))))

(defmethod handler "/newgame" [req]
  (init-monsters)
  (init-player)
  (reset! *turn-counter* 1)
  (redirect "/main"))

(defmethod handler "/" [req]
  (response-html (html [:h1 "Hello World from Ring and Hiccup!"]
                       [:a {:href "newgame"} "New Game"])))

(defn apply-monster-attacks [monsters]
  (clojure.string/join
   "<br/>"
   (filter (comp not true?) (map #(or (monster-dead %) (monster-attack %)) monsters))))

(defmethod handler :default [req]
  (let [resp (follow-link req)]
    (if (= 0 (mod @*turn-counter* 3))
      (update-in resp [:flash] str (apply-monster-attacks @*monsters*))
      resp)))

(defn immutant-handler [request]
  (handler request))

(def app (-> handler
	     (wrap-flash)
	     (wrap-session {:cookie-attrs {:path "/"} :store (memory-store)})
	     (wrap-keyword-params)
	     (wrap-params)
	     (wrap-reload '(orc_battle_online.core))
	     (wrap-stacktrace)))

(defn boot []
  (init-monsters)
  (init-player)
  (run-jetty app {:port 8000 :join? false}))

(defn -main [& args]
  (boot))