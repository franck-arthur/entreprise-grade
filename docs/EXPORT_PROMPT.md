# Export hebdomadaire de questionnaires – Architecture Spring Batch / PostgreSQL
## Excel comme dictionnaire contractuel et moteur de codification

---

## 1. Contexte général

Nous devons produire **3 exports CSV hebdomadaires** à partir de données de questionnaires stockées en **PostgreSQL**.

| CSV | Nombre de colonnes | Feuille Excel |
|----|-------------------|---------------|
| CSV 1 | ~144 colonnes | Feuille 1 |
| CSV 2 | ~14 colonnes | Feuille 2 |
| CSV 3 | ~500 colonnes | Feuille 3 |

Les exports doivent être :
- performants
- robustes
- maintenables
- pilotés par un **fichier Excel unique**

---

## 2. Rôle du fichier Excel

Le fichier Excel est le **contrat fonctionnel officiel** des exports CSV.

Il définit :
- les colonnes à produire
- l’ordre des colonnes
- les règles de gestion
- les codifications des valeurs
- les formats attendus

👉 **Aucune règle métier ne doit être codée en dur dans le code Java.**

---

## 3. Structure du fichier Excel

### 3.1 Organisation

Le fichier Excel contient **3 feuilles**, une par CSV :

| Feuille | CSV cible |
|-------|-----------|
| `CSV_1` | CSV 1 |
| `CSV_2` | CSV 2 |
| `CSV_3` | CSV 3 |

---

### 3.2 En-têtes des feuilles Excel

Chaque feuille contient les colonnes suivantes :

| Colonne Excel | Description |
|--------------|------------|
| `ordre_colonne` | Ordre de la colonne dans le CSV |
| `code_variable` | Code technique de la colonne CSV |
| `libelle` | Libellé fonctionnel |
| `format` | Format attendu (NUM, TEXTE, DATE…) |
| `type_question` | LISTE, TEXTE, BOOLEEN, NUMERIQUE… |
| `regle_gestion` | Règle de transformation/codification |
| `valeur_possible_1` | Valeur possible (index 1) |
| `valeur_possible_2` | Valeur possible (index 2) |
| `valeur_possible_3` | ... |
| `valeur_possible_n` | Valeur possible |

👉 **Chaque ligne correspond à une colonne du CSV**  
👉 **L’ordre des lignes correspond à l’ordre des colonnes dans le CSV**

---

### 3.3 Règle de gestion standard (RG principale)

**RG_POSITION_VALEUR_POSSIBLE**

> Lorsque la réponse correspond à `valeur_possible_1`, la valeur exportée dans le CSV est `1`,  
> lorsque la réponse correspond à `valeur_possible_2`, la valeur exportée est `2`,  
> et ainsi de suite.

Cette règle est la règle **par défaut** pour :
- les questions de type `LISTE`
- toute colonne ayant des `valeur_possible_x` définies

---

### 3.4 Exemple de ligne Excel

| code_variable | libelle | format | type_question | regle_gestion | valeur_possible_1 | valeur_possible_2 |
|--------------|---------|--------|---------------|---------------|-------------------|-------------------|
| Q_SEXE | Sexe | NUM | LISTE | POSITION_VALEUR_POSSIBLE | Homme | Femme |
| Q_EMAIL | Email | TEXTE | TEXTE |  |  |  |

Résultat attendu dans le CSV :
- Homme → `1`
- Femme → `2`
- Email → `valeur récupérée en base suivant la règle de gestion`
---

## 4. Modélisation PostgreSQL

### 4.1 Table de staging Excel

```sql
CREATE TABLE excel_csv_definition_stg (
    nom_csv TEXT,
    ordre_colonne INTEGER,
    code_variable TEXT,
    libelle TEXT,
    format TEXT,
    type_question TEXT,
    regle_gestion TEXT,
    index_valeur INTEGER,
    valeur_possible TEXT,
    date_import TIMESTAMP,
    fichier_source TEXT
);
