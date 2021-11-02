<a name="top"></a>

# Core2

#### Index

1. [Installation](#install)
2. [Quickstart](#quickstart)
3. [Model](#model)
4. [Permissions](#permissions)
5. [Data Events](#data-events)
6. [API](#api)

Core2 is thin wrapper library around the amazing [XTDB](https://xtdb.com/) with some added functionality.

**Core2 is not:**

1. An attempt to change any innate behavior in XTDB
2. A framework
3. Too opinionated
4. A bulletproof, keep-you-from-shooting-yourself-in-the-foot, all-in-one solution to data in Clojure.

**Core2 is (on top of XTDB):**

1. Automatic [data validation](#model) on create and update
2. A user/group [permissions model](#permissions) for controlling access to documents
3. A [Data Event](#data-events) response system that always provides consistent responses, as opposed to throwing Exceptions.
4. A permissions-controlled, time-aware, bi-temporal database solution in Clojure/Java.

<a name="install"></a>
# Installation

### Leiningen

```clojure
[core2 "0.1"]
```

### Deps.edn

```clojure
core2/core2 {:mvn/version "0.1"}
```

<a name="quickstart"></a>
# Quickstart

#### Clojure

```clojure
(require '[core2.config :as config])
(require '[core2.db :as db])
(require '[core2.api :as core2])

(config/init!)
(db/start-db)

(core2/create-initial-user! {:user/email "system@your-site.com"
                             :user/first_name "System"
                             :user/last_name "User"}))
                             
...
;TODO: add example
```

[Back to top](#top)

<a name="model"></a>
# Model

> _TODO:_ add docs

[Back to top](#top)

<a name="permissions"></a>
# Permissions

> _TODO:_ add docs

[Back to top](#top)

<a name="data-events"></a>
# Data Events

> _TODO:_ add docs

[Back to top](#top)

<a name="api"></a>
# API

> _TODO:_ add docs

[Back to top](#top)