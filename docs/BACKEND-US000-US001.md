# Backend — US-000 et US-001

Le backend expose uniquement l’authentification, l’administration de la plateforme et la consultation d’un tenant par ses membres. Les anciennes API produits, paniers, studios, activités, affectations, offres et dashboards ont été retirées. Le frontend qui utilisait ces routes devra être adapté séparément.

Aucun seed, compte, module, schéma tenant ou contenu métier n’est ajouté au démarrage. Aucun RLS ni contexte PostgreSQL `app.*` n’est utilisé. `docs/script.sql`, déjà exécuté, reste le script initial des neuf tables publiques : ne pas le relancer sur la base existante.

## Connexion et permissions

`backend/src/main/resources/application-dev.yml` pointe vers `jdbc:postgresql://127.0.0.1:5432/B2B_Market`. Les variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD` peuvent remplacer cette configuration. `spring.application.name` désigne l’application, pas la base de données.

La vérification en lecture seule du 31 octobre 2026 a confirmé que le compte configuré `app_rls` peut se connecter à `B2B_Market`, mais ne possède pas les droits sur les neuf tables publiques, leurs séquences, ni le droit de créer des schémas. **Le propriétaire de la base doit accorder ces permissions avant d’utiliser les API.** Le nom `app_rls` peut être conservé ; aucune politique RLS n’est nécessaire.

À exécuter manuellement par le propriétaire, connecté à `B2B_Market` (ces commandes ne créent aucune donnée) :

```sql
GRANT CREATE ON DATABASE "B2B_Market" TO app_rls;
GRANT USAGE ON SCHEMA public TO app_rls;
GRANT SELECT, INSERT ON public.users, public.user_roles,
    public.tenant_memberships, public.tenants, public.modules,
    public.tenant_modules TO app_rls;
-- Nécessaire aux verrous SELECT ... FOR UPDATE.
GRANT UPDATE ON public.users, public.tenants TO app_rls;
-- Nécessaire aux FK des nouvelles tables tenant.
GRANT REFERENCES ON public.users, public.process TO app_rls;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_rls;
```

Les schémas créés par l’API appartiennent au compte de connexion. Celui-ci n’a pas besoin d’être superutilisateur ni de pouvoir créer des bases. L’isolation métier est contrôlée dans l’application ; le compte technique a accès aux schémas qu’il provisionne.

## Premier Platform Admin, sans seed

1. Enregistrer le véritable utilisateur avec `POST /api/auth/register` et le rôle `STUDENT`.
2. Le propriétaire de la base lui attribue manuellement le rôle global `PLATFORM_ADMIN`. Remplacer l’email ci-dessous par celui de ce compte réel :

```sql
INSERT INTO public.user_roles (user_id, tenant_id, role)
SELECT id, NULL, 'PLATFORM_ADMIN'
FROM public.users
WHERE email = 'email-du-compte-reel' AND is_active
ON CONFLICT (user_id, role) WHERE tenant_id IS NULL DO NOTHING;
```

3. Se connecter avec `/api/auth/login`. Utiliser le JWT avec `Authorization: Bearer <token>`, ou le bouton **Authorize** dans Swagger.

Cette procédure est explicite et manuelle. Il n’existe aucun endpoint public de promotion, aucun mot de passe par défaut et aucun bootstrap automatique. Un administrateur de tenant doit également être un utilisateur actif existant. Son identifiant est disponible dans `public.users.id`.

## API conservées et ajoutées

| Méthode | Route | Accès / effet |
|---|---|---|
| POST | `/api/auth/register` | Public ; inscription `STUDENT` uniquement, sans tenant |
| POST | `/api/auth/login` | Public ; connexion pour les cinq rôles |
| POST | `/api/auth/otp/request` | Public ; envoi du code via SMTP configuré |
| POST | `/api/auth/otp/verify` | Public ; vérification du code et émission du JWT |
| GET | `/api/auth/me` | Utilisateur actif authentifié |
| GET | `/api/health` | Public ; état de l’application |
| POST | `/api/platform/tenants` | Platform Admin ; création du tenant et attribution de son administrateur |
| GET | `/api/platform/tenants` | Platform Admin ; liste des tenants |
| GET | `/api/platform/tenants/{id}` | Platform Admin ; tenant et modules |
| POST | `/api/platform/modules` | Platform Admin ; création d’une entrée du catalogue |
| GET | `/api/platform/modules` | Platform Admin ; liste du catalogue |
| GET | `/api/platform/tenants/{id}/modules` | Platform Admin ; modules attribués |
| POST | `/api/platform/module-assignments` | Platform Admin ; attribution additive et provisionnement |
| GET | `/api/tenants/{id}` | Membre possédant un rôle dans ce tenant ; tenant et modules |

Les rôles et l’activation du compte sont relus en base pour chaque requête authentifiée. Une suppression de rôle ou une désactivation prend effet même avec un ancien JWT. Les en-têtes `X-Active-Role` et `X-Tenant-Id` ne sont plus nécessaires et n’accordent aucun droit. Un Platform Admin utilise les routes `/api/platform` ; l’accès `/api/tenants/{id}` exige une appartenance à ce tenant.

### US-000

```http
POST /api/platform/tenants
Content-Type: application/json
Authorization: Bearer <token>

{"name":"Entreprise","tenantAdminUserId":42}
```

Réponse `201` : `id`, `name`, `schemaName`, `status`. Le statut est `PENDING_SETUP`. Le nom du schéma est généré par le serveur à partir d’un UUID ; aucun identifiant SQL n’est accepté dans la requête. Seuls les champs de l’entreprise déjà prévus au schéma sont exposés.

La création du tenant, de l’appartenance et du rôle `TENANT_ADMIN` est atomique. Les autres rôles de l’utilisateur sont conservés. Un utilisateur déjà Tenant Admin reçoit une erreur `409`, conformément à la contrainte d’un seul tenant administré par utilisateur. Aucun schéma tenant n’est créé à cette étape.

### US-001

Créer d’abord les modules souhaités, individuellement :

```http
POST /api/platform/modules

{"name":"Studios","code":"STUDIOS","description":"Gestion des studios"}
```

Les codes sont normalisés en majuscules, uniques, limités à 40 caractères et composés de lettres, chiffres et underscores, avec une lettre initiale. Un code dupliqué renvoie `409`. `provisioningSupported` indique si des migrations sont disponibles. Un nouveau code métier peut être enregistré, mais ne peut être attribué tant que ses migrations ne sont pas implémentées.

```http
POST /api/platform/module-assignments

{
  "tenantIds":["UUID-TENANT-1","UUID-TENANT-2"],
  "moduleCodes":["STUDIOS","ACTIVITIES"]
}
```

Chaque module demandé est ajouté à chaque tenant demandé. La réponse `200` contient une liste d’objets `{tenant, modules}`. Les doublons de la requête sont ignorés. Les modules précédemment attribués restent actifs. Une réattribution réussit sans recréer les tables ni modifier les données existantes. Le statut reste `PENDING_SETUP` : aucune transition d’activation commerciale n’est définie dans ces deux stories.

| Migration | Tables créées |
|---|---|
| Socle commun | `addresses`, `address_admins`, `employee_addresses` |
| `PRODUCTS_BASKETS` | `products`, `product_batches`, `baskets`, `basket_items` |
| `STUDIOS` | `studios` |
| `ACTIVITIES` | `activities` |

Chaque schéma reçoit aussi `schema_migrations`, table technique contenant le module, la version, le checksum SHA-256 et la date d’application. Les migrations sont dans `backend/src/main/resources/db/tenant/`. Elles reprennent les modèles de `docs/script.sql` et ne contiennent aucune donnée métier. Elles sont déclenchées uniquement par l’API d’attribution, jamais au démarrage.

Le service verrouille les tenants dans un ordre stable, applique le socle puis les modules et enregistre les associations dans la même transaction PostgreSQL. Un échec annule toute la demande, y compris les schémas et tables nouvellement créés pour les autres tenants de cette demande.

Ne pas modifier une migration déjà appliquée : son checksum est vérifié lors des attributions suivantes. Ajouter une nouvelle version et l’enregistrer dans le service de provisionnement pour une évolution. Les anciennes tables créées manuellement sans historique ne sont pas adoptées ou écrasées automatiquement ; un conflit provoque l’annulation de la demande.

Erreurs principales : `400` requête invalide, `401` authentification absente/invalide, `403` accès refusé, `404` tenant/module inconnu, `409` doublon, migration indisponible ou conflit de structure, `503` permissions PostgreSQL insuffisantes. Aucun SQL interne ni identifiant de connexion n’est retourné dans ces erreurs.

## Vérification

```bash
mvn -f backend/pom.xml test
backend/scripts/test-postgres.sh
```

La première commande exécute les tests unitaires. La seconde exige les outils PostgreSQL locaux (`initdb`, `pg_ctl`, `psql`), Python 3, Java 21 et Maven. `PG_BIN` permet de choisir leur répertoire. Elle crée un cluster temporaire dans `/tmp`, utilise un compte applicatif non-superutilisateur, exécute les tests HTTP et supprime le cluster à la fin. Elle ne se connecte jamais à `B2B_Market`. Les comptes et données de test existent uniquement dans ce cluster éphémère.

Les tests couvrent l’absence de seed au démarrage, l’authentification et l’OTP, les droits et la révocation des rôles, les créations concurrentes, l’isolation, les modules sélectionnés, les réattributions, la préservation des données, les checksums et l’annulation transactionnelle des migrations. Les snapshots OpenAPI sont générés dans `backend/target/openapi.json` et `backend/target/openapi.yaml` ; les versions publiées dans `docs/` doivent être recopiées après une évolution de l’API.
